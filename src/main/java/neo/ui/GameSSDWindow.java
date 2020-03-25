package neo.ui;

import static neo.Renderer.Material.SS_GUI;
import static neo.TempDump.atoi;
import static neo.framework.DeclManager.declManager;
import static neo.framework.KeyInput.K_MOUSE1;
import static neo.framework.KeyInput.K_MOUSE2;
import static neo.framework.Session.session;
import static neo.idlib.Lib.Max;
import static neo.idlib.Lib.Min;
import static neo.idlib.Lib.colorWhite;
import static neo.idlib.Text.Str.va;
import static neo.idlib.math.Math_h.DEG2RAD;
import static neo.sys.sys_public.sysEventType_t.SE_KEY;
import static neo.ui.DeviceContext.VIRTUAL_HEIGHT;
import static neo.ui.DeviceContext.VIRTUAL_WIDTH;
import static neo.ui.GameSSDWindow.SSD.SSD_ENTITY_ASTEROID;
import static neo.ui.GameSSDWindow.SSD.SSD_ENTITY_ASTRONAUT;
import static neo.ui.GameSSDWindow.SSD.SSD_ENTITY_BASE;
import static neo.ui.GameSSDWindow.SSD.SSD_ENTITY_EXPLOSION;
import static neo.ui.GameSSDWindow.SSD.SSD_ENTITY_POINTS;
import static neo.ui.GameSSDWindow.SSD.SSD_ENTITY_POWERUP;
import static neo.ui.GameSSDWindow.SSD.SSD_ENTITY_PROJECTILE;

import java.nio.ByteBuffer;

import neo.TempDump.SERiAL;
import neo.Renderer.Material.idMaterial;
import neo.framework.File_h.idFile;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Text.Parser.idParser;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Random.idRandom;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import neo.sys.sys_public.sysEvent_s;
import neo.ui.DeviceContext.idDeviceContext;
import neo.ui.Rectangle.idRectangle;
import neo.ui.SimpleWindow.drawWin_t;
import neo.ui.UserInterfaceLocal.idUserInterfaceLocal;
import neo.ui.Window.idWindow;
import neo.ui.Winvar.idWinBool;
import neo.ui.Winvar.idWinVar;

/**
 *
 */
public class GameSSDWindow {

    public static final float Z_NEAR = 100.0f;
    public static final float Z_FAR = 4000.0f;
    public static final int ENTITY_START_DIST = 3000;
//
    public static final float V_WIDTH = 640.0f;
    public static final float V_HEIGHT = 480.0f;
//
    /*
     *****************************************************************************
     * SSDCrossHair
     ****************************************************************************
     */
    public static final String CROSSHAIR_STANDARD_MATERIAL = "game/SSD/crosshair_standard";
    public static final String CROSSHAIR_SUPER_MATERIAL = "game/SSD/crosshair_super";
//    

    public static class SSDCrossHair {

//	enum {
        public static final int CROSSHAIR_STANDARD = 0;
        public static final int CROSSHAIR_SUPER = 1;
        public static final int CROSSHAIR_COUNT = 2;
//	};
        public idMaterial[] crosshairMaterial = new idMaterial[CROSSHAIR_COUNT];
        int currentCrosshair;
        float crosshairWidth, crosshairHeight;
//

        public SSDCrossHair() {
        }
//				~SSDCrossHair();

        public void WriteToSaveGame(idFile savefile) {

            savefile.WriteInt(this.currentCrosshair);
            savefile.WriteFloat(this.crosshairWidth);
            savefile.WriteFloat(this.crosshairHeight);

        }

        public void ReadFromSaveGame(idFile savefile) {

            InitCrosshairs();

            this.currentCrosshair = savefile.ReadInt();
            this.crosshairWidth = savefile.ReadFloat();
            this.crosshairHeight = savefile.ReadFloat();

        }

        public void InitCrosshairs() {

            this.crosshairMaterial[CROSSHAIR_STANDARD] = declManager.FindMaterial(CROSSHAIR_STANDARD_MATERIAL);
            this.crosshairMaterial[CROSSHAIR_SUPER] = declManager.FindMaterial(CROSSHAIR_SUPER_MATERIAL);

            this.crosshairWidth = 64;
            this.crosshairHeight = 64;

            this.currentCrosshair = CROSSHAIR_STANDARD;

        }

        public void Draw(idDeviceContext dc, final idVec2 cursor) {

            dc.DrawMaterial(cursor.x - (this.crosshairWidth / 2), cursor.y - (this.crosshairHeight / 2),
                    this.crosshairWidth, this.crosshairHeight,
                    this.crosshairMaterial[this.currentCrosshair], colorWhite, 1.0f, 1.0f);
        }
    }

    public enum SSD {

        SSD_ENTITY_BASE,//= 0,
        SSD_ENTITY_ASTEROID,
        SSD_ENTITY_ASTRONAUT,
        SSD_ENTITY_EXPLOSION,
        SSD_ENTITY_POINTS,
        SSD_ENTITY_PROJECTILE,
        SSD_ENTITY_POWERUP
    }

    /*
     *****************************************************************************
     * SSDEntity	
     ****************************************************************************
     */
    public static class SSDEntity {

        //SSDEntity Information
        public SSD type;
        public int id;
        public idStr materialName;
        public idMaterial material;
        public idVec3 position;
        public idVec2 size;
        public float radius;
        public float hitRadius;
        public float rotation;
//
        public idVec4 matColor;
//
        public idStr text;
        public float textScale;
        public idVec4 foreColor;
//
        public idGameSSDWindow game;
        public int currentTime;
        public int lastUpdate;
        public int elapsed;
//
        public boolean destroyed;
        public boolean noHit;
        public boolean noPlayerDamage;
//
        public boolean inUse;
//
//	

        public SSDEntity() {
            EntityInit();
        }
//	virtual				~SSDEntity();

        public void WriteToSaveGame(idFile savefile) {

            savefile.WriteInt(this.type);
            this.game.WriteSaveGameString(this.materialName, savefile);
            savefile.Write(this.position);
            savefile.Write(this.size);
            savefile.WriteFloat(this.radius);
            savefile.WriteFloat(this.hitRadius);
            savefile.WriteFloat(this.rotation);

            savefile.Write(this.matColor);

            this.game.WriteSaveGameString(this.text, savefile);
            savefile.WriteFloat(this.textScale);
            savefile.Write(this.foreColor);

            savefile.WriteInt(this.currentTime);
            savefile.WriteInt(this.lastUpdate);
            savefile.WriteInt(this.elapsed);

            savefile.WriteBool(this.destroyed);
            savefile.WriteBool(this.noHit);
            savefile.WriteBool(this.noPlayerDamage);

            savefile.WriteBool(this.inUse);

        }

        public void ReadFromSaveGame(idFile savefile, idGameSSDWindow _game) {

            this.type = SSD.values()[savefile.ReadInt()];
            this.game.ReadSaveGameString(this.materialName, savefile);
            SetMaterial(this.materialName.getData());
            savefile.Read(this.position);
            savefile.Read(this.size);
            this.radius = savefile.ReadFloat();
            this.hitRadius = savefile.ReadFloat();
            this.rotation = savefile.ReadFloat();

            savefile.Read(this.matColor);

            this.game.ReadSaveGameString(this.text, savefile);
            this.textScale = savefile.ReadFloat();
            savefile.Read(this.foreColor);

            this.game = _game;
            this.currentTime = savefile.ReadInt();
            this.lastUpdate = savefile.ReadInt();
            this.elapsed = savefile.ReadInt();

            this.destroyed = savefile.ReadBool();
            this.noHit = savefile.ReadBool();
            this.noPlayerDamage = savefile.ReadBool();

            this.inUse = savefile.ReadBool();
        }

        public void EntityInit() {

            this.inUse = false;

            this.type = SSD_ENTITY_BASE;

            this.materialName = new idStr("");
            this.material = null;
            this.position.Zero();
            this.size.Zero();
            this.radius = 0.0f;
            this.hitRadius = 0.0f;
            this.rotation = 0.0f;

            this.currentTime = 0;
            this.lastUpdate = 0;

            this.destroyed = false;
            this.noHit = false;
            this.noPlayerDamage = false;

            this.matColor.Set(1, 1, 1, 1);

            this.text = new idStr("");
            this.textScale = 1.0f;
            this.foreColor.Set(1, 1, 1, 1);
        }

        public void SetGame(idGameSSDWindow _game) {
            this.game = _game;
        }

        public void SetMaterial(final String _name) {
            this.materialName.oSet(_name);
            this.material = declManager.FindMaterial(_name);
            this.material.SetSort(SS_GUI);
        }

        public void SetPosition(final idVec3 _position) {
            this.position = _position;//TODO:is this by value, or by reference?
        }

        public void SetSize(final idVec2 _size) {
            this.size = _size;
        }

        public void SetRadius(float _radius, float _hitFactor /*= 1.0f*/) {
            this.radius = _radius;
            this.hitRadius = _radius * _hitFactor;
        }

        public void SetRotation(float _rotation) {
            this.rotation = _rotation;
        }

        public void Update() {

            this.currentTime = this.game.ssdTime;

            //Is this the first update
            if (this.lastUpdate == 0) {
                this.lastUpdate = this.currentTime;
                return;
            }

            this.elapsed = this.currentTime - this.lastUpdate;

            EntityUpdate();

            this.lastUpdate = this.currentTime;
        }

        public boolean HitTest(final idVec2 pt) {

            if (this.noHit) {
                return false;
            }

            final idVec3 screenPos = WorldToScreen(this.position);

            //Scale the radius based on the distance from the player
            final float scale = 1.0f - ((screenPos.z - Z_NEAR) / (Z_FAR - Z_NEAR));
            final float scaledRad = scale * this.hitRadius;

            //So we can compare against the square of the length between two points
            final float scaleRadSqr = scaledRad * scaledRad;

            final idVec2 diff = screenPos.ToVec2().oMinus(pt);
            final float dist = idMath.Fabs(diff.LengthSqr());

            if (dist < scaleRadSqr) {
                return true;
            }
            return false;
        }

        public void EntityUpdate() {
        }

        public void Draw(idDeviceContext dc) {

            final idVec2 persize = new idVec2();
            float x, y;

            final idBounds bounds = new idBounds();
            bounds.oSet(0, new idVec3(this.position.x - (this.size.x / 2.0f), this.position.y - (this.size.y / 2.0f), this.position.z));
            bounds.oSet(1, new idVec3(this.position.x + (this.size.x / 2.0f), this.position.y + (this.size.y / 2.0f), this.position.z));

            final idBounds screenBounds = WorldToScreen(bounds);
            persize.x = idMath.Fabs(screenBounds.oGet(1).x - screenBounds.oGet(0).x);
            persize.y = idMath.Fabs(screenBounds.oGet(1).y - screenBounds.oGet(0).y);

//	idVec3 center = screenBounds.GetCenter();
            x = screenBounds.oGet(0).x;
            y = screenBounds.oGet(1).y;
            dc.DrawMaterialRotated(x, y, persize.x, persize.y, this.material, this.matColor, 1.0f, 1.0f, DEG2RAD(this.rotation));

            if (this.text.Length() > 0) {
                final idRectangle rect = new idRectangle(x, y, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
                dc.DrawText(this.text.getData(), this.textScale, 0, this.foreColor, rect, false);
            }

        }

        public void DestroyEntity() {
            this.inUse = false;
        }

        public void OnHit(int key) {
        }

        public void OnStrikePlayer() {
        }

        public idBounds WorldToScreen(final idBounds worldBounds) {

            final idVec3 screenMin = WorldToScreen(worldBounds.oGet(0));
            final idVec3 screenMax = WorldToScreen(worldBounds.oGet(1));

            final idBounds screenBounds = new idBounds(screenMin, screenMax);
            return screenBounds;
        }

        public idVec3 WorldToScreen(final idVec3 worldPos) {

            final float d = 0.5f * V_WIDTH * idMath.Tan(DEG2RAD(90.0f) / 2.0f);

            //World To Camera Coordinates
            final idVec3 cameraTrans = new idVec3(0, 0, d);
            idVec3 cameraPos;
            cameraPos = worldPos.oPlus(cameraTrans);

            //Camera To Screen Coordinates
            final idVec3 screenPos = new idVec3();
            screenPos.x = ((d * cameraPos.x) / cameraPos.z) + ((0.5f * V_WIDTH) - 0.5f);
            screenPos.y = ((-d * cameraPos.y) / cameraPos.z) + ((0.5f * V_HEIGHT) - 0.5f);
            screenPos.z = cameraPos.z;

            return screenPos;
        }

        public idVec3 ScreenToWorld(final idVec3 screenPos) {

            final idVec3 worldPos = new idVec3();

            worldPos.x = screenPos.x - (0.5f * V_WIDTH);
            worldPos.y = -(screenPos.y - (0.5f * V_HEIGHT));
            worldPos.z = screenPos.z;

            return worldPos;
        }
    }

    /*
     *****************************************************************************
     * SSDMover	
     ****************************************************************************
     */
    public static class SSDMover extends SSDEntity {

        public idVec3 speed;
        public float rotationSpeed;
//

        public SSDMover() {
        }
        // virtual				~SSDMover();

        @Override
        public void WriteToSaveGame(idFile savefile) {
            super.WriteToSaveGame(savefile);

            savefile.Write(this.speed);
            savefile.WriteFloat(this.rotationSpeed);
        }

        @Override
        public void ReadFromSaveGame(idFile savefile, idGameSSDWindow _game) {
            super.ReadFromSaveGame(savefile, _game);

            savefile.Read(this.speed);
            this.rotationSpeed = savefile.ReadFloat();
        }

        public void MoverInit(final idVec3 _speed, float _rotationSpeed) {

            this.speed = _speed;
            this.rotationSpeed = _rotationSpeed;
        }

        @Override
        public void EntityUpdate() {

            super.EntityUpdate();

            //Move forward based on speed (units per second)
            final idVec3 moved = this.speed.oMultiply(this.elapsed / 1000.0f);
            this.position.oPluSet(moved);

            final float rotated = (this.elapsed / 1000.0f) * this.rotationSpeed * 360.0f;
            this.rotation += rotated;
            if (this.rotation >= 360) {
                this.rotation -= 360.0f;
            }
            if (this.rotation < 0) {
                this.rotation += 360.0f;
            }
        }
    }
    /*
     *****************************************************************************
     * SSDAsteroid	
     ****************************************************************************
     */
    public static final int MAX_ASTEROIDS = 64;
    public static final String ASTEROID_MATERIAL = "game/SSD/asteroid";

    public static class SSDAsteroid extends SSDMover {

        public int health;

        public SSDAsteroid() {
        }
        // ~SSDAsteroid();

        @Override
        public void WriteToSaveGame(idFile savefile) {
            super.WriteToSaveGame(savefile);

            savefile.WriteInt(this.health);
        }

        @Override
        public void ReadFromSaveGame(idFile savefile, idGameSSDWindow _game) {
            super.ReadFromSaveGame(savefile, _game);

            this.health = savefile.ReadInt();
        }

        public void Init(idGameSSDWindow _game, final idVec3 startPosition, final idVec2 _size, float _speed, float rotate, int _health) {

            EntityInit();
            MoverInit(new idVec3(0, 0, -_speed), rotate);

            SetGame(_game);

            this.type = SSD_ENTITY_ASTEROID;

            SetMaterial(ASTEROID_MATERIAL);
            SetSize(_size);
            SetRadius(Max(this.size.x, this.size.y), 0.3f);
            SetRotation(this.game.random.RandomInt(360));

            this.position = startPosition;

            this.health = _health;
        }

        @Override
        public void EntityUpdate() {

            super.EntityUpdate();
        }

        public static SSDAsteroid GetNewAsteroid(idGameSSDWindow _game, final idVec3 startPosition, final idVec2 _size, float _speed, float rotate, int _health) {
            for (int i = 0; i < MAX_ASTEROIDS; i++) {
                if (!asteroidPool[i].inUse) {
                    asteroidPool[i].Init(_game, startPosition, _size, _speed, rotate, _health);
                    asteroidPool[i].inUse = true;
                    asteroidPool[i].id = i;

                    return asteroidPool[i];
                }
            }
            return null;
        }

        public static SSDAsteroid GetSpecificAsteroid(int id) {
            return asteroidPool[id];
        }

        public static void WriteAsteroids(idFile savefile) {
            int count = 0;
            for (int i = 0; i < MAX_ASTEROIDS; i++) {
                if (asteroidPool[i].inUse) {
                    count++;
                }
            }
            savefile.WriteInt(count);
            for (int i = 0; i < MAX_ASTEROIDS; i++) {
                if (asteroidPool[i].inUse) {
                    savefile.WriteInt(asteroidPool[i].id);
                    asteroidPool[i].WriteToSaveGame(savefile);
                }
            }
        }

        public static void ReadAsteroids(idFile savefile, idGameSSDWindow _game) {

            int count;
            count = savefile.ReadInt();
            for (int i = 0; i < count; i++) {
                int id;
                id = savefile.ReadInt();
                final SSDAsteroid ent = GetSpecificAsteroid(id);
                ent.ReadFromSaveGame(savefile, _game);
            }
        }
//        
        protected static final SSDAsteroid[] asteroidPool = new SSDAsteroid[MAX_ASTEROIDS];
    }
    /*
     *****************************************************************************
     * SSDAstronaut	
     ****************************************************************************
     */
    public static final int MAX_ASTRONAUT = 8;
    public static final String ASTRONAUT_MATERIAL = "game/SSD/astronaut";

    public static class SSDAstronaut extends SSDMover {

        public int health;

        public SSDAstronaut() {
        }
        // ~SSDAstronaut();

        @Override
        public void WriteToSaveGame(idFile savefile) {
            super.WriteToSaveGame(savefile);

            savefile.WriteInt(this.health);
        }

        @Override
        public void ReadFromSaveGame(idFile savefile, idGameSSDWindow _game) {
            super.ReadFromSaveGame(savefile, _game);

            this.health = savefile.ReadInt();
        }

        public void Init(idGameSSDWindow _game, final idVec3 startPosition, float _speed, float rotate, int _health) {

            EntityInit();
            MoverInit(new idVec3(0, 0, -_speed), rotate);

            SetGame(_game);

            this.type = SSD_ENTITY_ASTRONAUT;

            SetMaterial(ASTRONAUT_MATERIAL);
            SetSize(new idVec2(256, 256));
            SetRadius(Max(this.size.x, this.size.y), 0.3f);
            SetRotation(this.game.random.RandomInt(360));

            this.position = startPosition;
            this.health = _health;
        }

        public static SSDAstronaut GetNewAstronaut(idGameSSDWindow _game, final idVec3 startPosition, float _speed, float rotate, int _health) {
            for (int i = 0; i < MAX_ASTRONAUT; i++) {
                if (!astronautPool[i].inUse) {
                    astronautPool[i].Init(_game, startPosition, _speed, rotate, _health);
                    astronautPool[i].inUse = true;
                    astronautPool[i].id = i;
                    return astronautPool[i];
                }
            }
            return null;
        }

        public static SSDAstronaut GetSpecificAstronaut(int id) {
            return astronautPool[id];
        }

        public static void WriteAstronauts(idFile savefile) {
            int count = 0;
            for (int i = 0; i < MAX_ASTRONAUT; i++) {
                if (astronautPool[i].inUse) {
                    count++;
                }
            }
            savefile.WriteInt(count);
            for (int i = 0; i < MAX_ASTRONAUT; i++) {
                if (astronautPool[i].inUse) {
                    savefile.WriteInt(astronautPool[i].id);
                    astronautPool[i].WriteToSaveGame(savefile);
                }
            }
        }

        public static void ReadAstronauts(idFile savefile, idGameSSDWindow _game) {

            int count;
            count = savefile.ReadInt();
            for (int i = 0; i < count; i++) {
                int id;
                id = savefile.ReadInt();
                final SSDAstronaut ent = GetSpecificAstronaut(id);
                ent.ReadFromSaveGame(savefile, _game);
            }
        }
//        
        protected static final SSDAstronaut[] astronautPool = new SSDAstronaut[MAX_ASTRONAUT];
    }
    /*
     *****************************************************************************
     * SSDExplosion	
     ****************************************************************************
     */
    public static final int MAX_EXPLOSIONS = 64;
    public static final String[] explosionMaterials = {
        "game/SSD/fball",
        "game/SSD/teleport"
    };
    public static final int EXPLOSION_MATERIAL_COUNT = 2;

    public static class SSDExplosion extends SSDEntity {

        public idVec2 finalSize;
        public int length;
        public int beginTime;
        public int endTime;
        public int explosionType;
//
        //The entity that is exploding
        public SSDEntity buddy;
        public boolean killBuddy;
        public boolean followBuddy;
        // enum {
        public static final int EXPLOSION_NORMAL = 0;
        public static final int EXPLOSION_TELEPORT = 1;
        // };

        public SSDExplosion() {
            this.type = SSD_ENTITY_EXPLOSION;
        }
        // ~SSDExplosion();

        @Override
        public void WriteToSaveGame(idFile savefile) {
            super.WriteToSaveGame(savefile);

            savefile.Write(this.finalSize);
            savefile.WriteInt(this.length);
            savefile.WriteInt(this.beginTime);
            savefile.WriteInt(this.endTime);
            savefile.WriteInt(this.explosionType);

            savefile.WriteInt(this.buddy.type);
            savefile.WriteInt(this.buddy.id);

            savefile.WriteBool(this.killBuddy);
            savefile.WriteBool(this.followBuddy);
        }

        @Override
        public void ReadFromSaveGame(idFile savefile, idGameSSDWindow _game) {
            super.ReadFromSaveGame(savefile, _game);

            savefile.Read(this.finalSize);
            this.length = savefile.ReadInt();
            this.beginTime = savefile.ReadInt();
            this.endTime = savefile.ReadInt();
            this.explosionType = savefile.ReadInt();

            SSD type;
            int id;
            type = SSD.values()[savefile.ReadInt()];
            id = savefile.ReadInt();

            //Get a pointer to my buddy
            this.buddy = _game.GetSpecificEntity(type, id);

            this.killBuddy = savefile.ReadBool();
            this.followBuddy = savefile.ReadBool();
        }

        public void Init(idGameSSDWindow _game, final idVec3 _position, final idVec2 _size, int _length, int _type, SSDEntity _buddy, boolean _killBuddy /*= true*/, boolean _followBuddy /*= true*/) {

            EntityInit();

            SetGame(_game);

            this.type = SSD_ENTITY_EXPLOSION;
            this.explosionType = _type;

            SetMaterial(explosionMaterials[this.explosionType]);
            SetPosition(_position);
            this.position.z -= 50;

            this.finalSize = _size;
            this.length = _length;
            this.beginTime = this.game.ssdTime;
            this.endTime = this.beginTime + this.length;

            this.buddy = _buddy;
            this.killBuddy = _killBuddy;
            this.followBuddy = _followBuddy;

            //Explosion Starts from nothing and will increase in size until it gets to final size
            this.size.Zero();

            this.noPlayerDamage = true;
            this.noHit = true;
        }

        @Override
        public void EntityUpdate() {

            super.EntityUpdate();

            //Always set my position to my buddies position except change z to be on top
            if (this.followBuddy) {
                this.position = this.buddy.position;
                this.position.z -= 50;
            } else {
                //Only mess with the z if we are not following
                this.position.z = this.buddy.position.z - 50;
            }

            //Scale the image based on the time
            this.size = this.finalSize.oMultiply((this.currentTime - this.beginTime) / this.length);

            //Destroy myself after the explosion is done
            if (this.currentTime > this.endTime) {
                this.destroyed = true;

                if (this.killBuddy) {
                    //Destroy the exploding object
                    this.buddy.destroyed = true;
                }
            }
        }

        public static SSDExplosion GetNewExplosion(idGameSSDWindow _game, final idVec3 _position, final idVec2 _size, int _length, int _type, SSDEntity _buddy, boolean _killBuddy /*= true*/, boolean _followBuddy /*= true*/) {
            for (int i = 0; i < MAX_EXPLOSIONS; i++) {
                if (!explosionPool[i].inUse) {
                    explosionPool[i].Init(_game, _position, _size, _length, _type, _buddy, _killBuddy, _followBuddy);
                    explosionPool[i].inUse = true;
                    return explosionPool[i];
                }
            }
            return null;
        }

        public static SSDExplosion GetNewExplosion(idGameSSDWindow _game, final idVec3 _position, final idVec2 _size, int _length, int _type, SSDEntity _buddy, boolean _killBuddy /*= true*/) {
            return GetNewExplosion(_game, _position, _size, _length, _type, _buddy, _killBuddy, true);
        }

        public static SSDExplosion GetNewExplosion(idGameSSDWindow _game, final idVec3 _position, final idVec2 _size, int _length, int _type, SSDEntity _buddy) {
            return GetNewExplosion(_game, _position, _size, _length, _type, _buddy, true);
        }

        public static SSDExplosion GetSpecificExplosion(int id) {
            return explosionPool[id];
        }

        public static void WriteExplosions(idFile savefile) {
            int count = 0;
            for (int i = 0; i < MAX_EXPLOSIONS; i++) {
                if (explosionPool[i].inUse) {
                    count++;
                }
            }
            savefile.WriteInt(count);
            for (int i = 0; i < MAX_EXPLOSIONS; i++) {
                if (explosionPool[i].inUse) {
                    savefile.WriteInt(explosionPool[i].id);
                    explosionPool[i].WriteToSaveGame(savefile);
                }
            }
        }

        public static void ReadExplosions(idFile savefile, idGameSSDWindow _game) {

            int count;
            count = savefile.ReadInt();
            for (int i = 0; i < count; i++) {
                int id;
                id = savefile.ReadInt();
                final SSDExplosion ent = GetSpecificExplosion(id);
                ent.ReadFromSaveGame(savefile, _game);
            }
        }
//        
        protected static final SSDExplosion[] explosionPool = new SSDExplosion[MAX_EXPLOSIONS];
    }
    /*
     *****************************************************************************
     * SSDPoints
     ****************************************************************************
     */
    public static final int MAX_POINTS = 16;

    public static class SSDPoints extends SSDEntity {

        int length;
        int distance;
        int beginTime;
        int endTime;
        idVec3 beginPosition;
        idVec3 endPosition;
        idVec4 beginColor;
        idVec4 endColor;

        public SSDPoints() {
            this.type = SSD_ENTITY_POINTS;
        }
        // ~SSDPoints();

        @Override
        public void WriteToSaveGame(idFile savefile) {
            super.WriteToSaveGame(savefile);

            savefile.WriteInt(this.length);
            savefile.WriteInt(this.distance);
            savefile.WriteInt(this.beginTime);
            savefile.WriteInt(this.endTime);

            savefile.Write(this.beginPosition);
            savefile.Write(this.endPosition);

            savefile.Write(this.beginColor);
            savefile.Write(this.endColor);

        }

        @Override
        public void ReadFromSaveGame(idFile savefile, idGameSSDWindow _game) {
            super.ReadFromSaveGame(savefile, _game);

            this.length = savefile.ReadInt();
            this.distance = savefile.ReadInt();
            this.beginTime = savefile.ReadInt();
            this.endTime = savefile.ReadInt();

            savefile.Read(this.beginPosition);
            savefile.Read(this.endPosition);

            savefile.Read(this.beginColor);
            savefile.Read(this.endColor);
        }

        public void Init(idGameSSDWindow _game, SSDEntity _ent, int _points, int _length, int _distance, final idVec4 color) {

            EntityInit();

            SetGame(_game);

            this.length = _length;
            this.distance = _distance;
            this.beginTime = this.game.ssdTime;
            this.endTime = this.beginTime + this.length;

            this.textScale = 0.4f;
            this.text = new idStr(va("%d", _points));

            float width = 0;
            for (int i = 0; i < this.text.Length(); i++) {
                width += this.game.GetDC().CharWidth(this.text.oGet(i), this.textScale);
            }

            this.size.Set(0, 0);

            //Set the start position at the top of the passed in entity
            this.position = WorldToScreen(_ent.position);
            this.position = ScreenToWorld(this.position);

            this.position.z = 0;
            this.position.x -= (width / 2.0f);

            this.beginPosition = this.position;

            this.endPosition = this.beginPosition;
            this.endPosition.y += _distance;

            //beginColor.Set(0,1,0,1);
            this.endColor.Set(1, 1, 1, 0);

            this.beginColor = color;
            this.beginColor.w = 1;

            this.noPlayerDamage = true;
            this.noHit = true;
        }

        @Override
        public void EntityUpdate() {

            final float t = (float) (this.currentTime - this.beginTime) / (float) this.length;

            //Move up from the start position
            this.position.Lerp(this.beginPosition, this.endPosition, t);

            //Interpolate the color
            this.foreColor.Lerp(this.beginColor, this.endColor, t);

            if (this.currentTime > this.endTime) {
                this.destroyed = true;
            }
        }

        public static SSDPoints GetNewPoints(idGameSSDWindow _game, SSDEntity _ent, int _points, int _length, int _distance, final idVec4 color) {
            for (int i = 0; i < MAX_POINTS; i++) {
                if (!pointsPool[i].inUse) {
                    pointsPool[i].Init(_game, _ent, _points, _length, _distance, color);
                    pointsPool[i].inUse = true;
                    return pointsPool[i];
                }
            }
            return null;
        }

        public static SSDPoints GetSpecificPoints(int id) {
            return pointsPool[id];
        }

        public static void WritePoints(idFile savefile) {
            int count = 0;
            for (int i = 0; i < MAX_POINTS; i++) {
                if (pointsPool[i].inUse) {
                    count++;
                }
            }
            savefile.WriteInt(count);
            for (int i = 0; i < MAX_POINTS; i++) {
                if (pointsPool[i].inUse) {
                    savefile.WriteInt(pointsPool[i].id);
                    pointsPool[i].WriteToSaveGame(savefile);
                }
            }
        }

        public static void ReadPoints(idFile savefile, idGameSSDWindow _game) {

            int count;
            count = savefile.ReadInt();
            for (int i = 0; i < count; i++) {
                int id;
                id = savefile.ReadInt();
                final SSDPoints ent = GetSpecificPoints(id);
                ent.ReadFromSaveGame(savefile, _game);
            }
        }
//
        protected static final SSDPoints[] pointsPool = new SSDPoints[MAX_POINTS];
    }
    /*
     *****************************************************************************
     * SSDProjectile
     ****************************************************************************
     */
    public static final int MAX_PROJECTILES = 64;
    public static final String PROJECTILE_MATERIAL = "game/SSD/fball";

    public static class SSDProjectile extends SSDEntity {

        idVec3 dir;
        idVec3 speed;
        int beginTime;
        int endTime;
        idVec3 endPosition;

        public SSDProjectile() {
            this.type = SSD_ENTITY_PROJECTILE;
        }
        // ~SSDProjectile();

        @Override
        public void WriteToSaveGame(idFile savefile) {
            super.WriteToSaveGame(savefile);

            savefile.Write(this.dir);
            savefile.Write(this.speed);
            savefile.WriteInt(this.beginTime);
            savefile.WriteInt(this.endTime);

            savefile.Write(this.endPosition);
        }

        @Override
        public void ReadFromSaveGame(idFile savefile, idGameSSDWindow _game) {
            super.ReadFromSaveGame(savefile, _game);

            savefile.Read(this.dir);
            savefile.Read(this.speed);
            this.beginTime = savefile.ReadInt();
            this.endTime = savefile.ReadInt();

            savefile.Read(this.endPosition);
        }

        public void Init(idGameSSDWindow _game, final idVec3 _beginPosition, final idVec3 _endPosition, float _speed, float _size) {

            EntityInit();

            SetGame(_game);

            SetMaterial(PROJECTILE_MATERIAL);
            this.size.Set(_size, _size);

            this.position = _beginPosition;
            this.endPosition = _endPosition;

            this.dir = _endPosition.oMinus(this.position);
            this.dir.Normalize();

            //speed.Zero();
            this.speed.x = this.speed.y = this.speed.z = _speed;

            this.noHit = true;
        }

        @Override
        public void EntityUpdate() {

            super.EntityUpdate();

            //Move forward based on speed (units per second)
            final idVec3 moved = this.dir.oMultiply((this.elapsed / 1000.0f) * this.speed.z);
            this.position.oPluSet(moved);

            if (this.position.z > this.endPosition.z) {
                //We have reached our position
                this.destroyed = true;
            }
        }

        public static SSDProjectile GetNewProjectile(idGameSSDWindow _game, final idVec3 _beginPosition, final idVec3 _endPosition, float _speed, float _size) {
            for (int i = 0; i < MAX_PROJECTILES; i++) {
                if (!projectilePool[i].inUse) {
                    projectilePool[i].Init(_game, _beginPosition, _endPosition, _speed, _size);
                    projectilePool[i].inUse = true;
                    return projectilePool[i];
                }
            }
            return null;
        }

        public static SSDProjectile GetSpecificProjectile(int id) {
            return projectilePool[id];
        }

        public static void WriteProjectiles(idFile savefile) {
            int count = 0;
            for (int i = 0; i < MAX_PROJECTILES; i++) {
                if (projectilePool[i].inUse) {
                    count++;
                }
            }
            savefile.WriteInt(count);
            for (int i = 0; i < MAX_PROJECTILES; i++) {
                if (projectilePool[i].inUse) {
                    savefile.WriteInt(projectilePool[i].id);
                    projectilePool[i].WriteToSaveGame(savefile);
                }
            }
        }

        public static void ReadProjectiles(idFile savefile, idGameSSDWindow _game) {

            int count;
            count = savefile.ReadInt();
            for (int i = 0; i < count; i++) {
                int id;
                id = savefile.ReadInt();
                final SSDProjectile ent = GetSpecificProjectile(id);
                ent.ReadFromSaveGame(savefile, _game);
            }
        }
//
        protected static final SSDProjectile[] projectilePool = new SSDProjectile[MAX_PROJECTILES];
    }
    /*
     *****************************************************************************
     * SSDPowerup
     ****************************************************************************
     */
    public static final String[][] powerupMaterials/*[][2]*/ = {
                {"game/SSD/powerupHealthClosed", "game/SSD/powerupHealthOpen"},
                {"game/SSD/powerupSuperBlasterClosed", "game/SSD/powerupSuperBlasterOpen"},
                {"game/SSD/powerupNukeClosed", "game/SSD/powerupNukeOpen"},
                {"game/SSD/powerupRescueClosed", "game/SSD/powerupRescueOpen"},
                {"game/SSD/powerupBonusPointsClosed", "game/SSD/powerupBonusPointsOpen"},
                {"game/SSD/powerupDamageClosed", "game/SSD/powerupDamageOpen"}};
//    
    public static final int POWERUP_MATERIAL_COUNT = 6;
//    
    public static final int MAX_POWERUPS = 64;
//    

    /**
     * Powerups work in two phases: 1.) Closed container hurls at you If you
     * shoot the container it open 3.) If an opened powerup hits the player he
     * aquires the powerup Powerup Types: Health - Give a specific amount of
     * health Super Blaster - Increases the power of the blaster (lasts a
     * specific amount of time) Asteroid Nuke - Destroys all asteroids on screen
     * as soon as it is aquired Rescue Powerup - Rescues all astronauts as soon
     * as it is acquited Bonus Points - Gives some bonus points when acquired
     */
    public static class SSDPowerup extends SSDMover {

//        enum POWERUP_STATE {
        static final int POWERUP_STATE_CLOSED = 0;
        static final int POWERUP_STATE_OPEN = 1;
//        };
//        enum POWERUP_TYPE {
        static final int POWERUP_TYPE_HEALTH = 0;
        static final int POWERUP_TYPE_SUPER_BLASTER = 1;
        static final int POWERUP_TYPE_ASTEROID_NUKE = 2;
        static final int POWERUP_TYPE_RESCUE_ALL = 3;
        static final int POWERUP_TYPE_BONUS_POINTS = 4;
        static final int POWERUP_TYPE_DAMAGE = 5;
        static final int POWERUP_TYPE_MAX = 6;
//        };
//
        int powerupState;
        int powerupType;
//

        public SSDPowerup() {
        }
        // virtual ~SSDPowerup();

        @Override
        public void WriteToSaveGame(idFile savefile) {
            super.WriteToSaveGame(savefile);

            savefile.WriteInt(this.powerupState);
            savefile.WriteInt(this.powerupType);
        }

        @Override
        public void ReadFromSaveGame(idFile savefile, idGameSSDWindow _game) {
            super.ReadFromSaveGame(savefile, _game);

            this.powerupState = savefile.ReadInt();
            this.powerupType = savefile.ReadInt();
        }

        @Override
        public void OnHit(int key) {

            if (this.powerupState == POWERUP_STATE_CLOSED) {

                //Small explosion to indicate it is opened
                final SSDExplosion explosion = SSDExplosion.GetNewExplosion(this.game, this.position, this.size.oMultiply(2.0f), 300, SSDExplosion.EXPLOSION_NORMAL, this, false, true);
                this.game.entities.Append(explosion);

                this.powerupState = POWERUP_STATE_OPEN;
                SetMaterial(powerupMaterials[this.powerupType][this.powerupState]);
            } else {
                //Destory the powerup with a big explosion
                final SSDExplosion explosion = SSDExplosion.GetNewExplosion(this.game, this.position, this.size.oMultiply(2), 300, SSDExplosion.EXPLOSION_NORMAL, this);
                this.game.entities.Append(explosion);
                this.game.PlaySound("arcade_explode");

                this.noHit = true;
                this.noPlayerDamage = true;
            }
        }

        @Override
        public void OnStrikePlayer() {

            if (this.powerupState == POWERUP_STATE_OPEN) {
                //The powerup was open so activate it
                OnActivatePowerup();
            }

            //Just destroy the powerup
            this.destroyed = true;
        }

        public void OnOpenPowerup() {
        }

        public void OnActivatePowerup() {
            switch (this.powerupType) {
                case POWERUP_TYPE_HEALTH: {
                    this.game.AddHealth(10);
                    break;
                }
                case POWERUP_TYPE_SUPER_BLASTER: {
                    this.game.OnSuperBlaster();
                    break;
                }
                case POWERUP_TYPE_ASTEROID_NUKE: {
                    this.game.OnNuke();
                    break;
                }
                case POWERUP_TYPE_RESCUE_ALL: {
                    this.game.OnRescueAll();
                    break;
                }
                case POWERUP_TYPE_BONUS_POINTS: {
                    final int points = (this.game.random.RandomInt(5) + 1) * 100;
                    this.game.AddScore(this, points);
                    break;
                }
                case POWERUP_TYPE_DAMAGE: {
                    this.game.AddDamage(10);
                    this.game.PlaySound("arcade_explode");
                    break;
                }

            }
        }

        public void Init(idGameSSDWindow _game, float _speed, float _rotation) {

            EntityInit();
            MoverInit(new idVec3(0, 0, -_speed), _rotation);

            SetGame(_game);
            SetSize(new idVec2(200, 200));
            SetRadius(Max(this.size.x, this.size.y), 0.3f);

            this.type = SSD_ENTITY_POWERUP;

            final idVec3 startPosition = new idVec3();
            startPosition.x = this.game.random.RandomInt(V_WIDTH) - (V_WIDTH / 2.0f);
            startPosition.y = this.game.random.RandomInt(V_HEIGHT) - (V_HEIGHT / 2.0f);
            startPosition.z = ENTITY_START_DIST;

            this.position = startPosition;
            //SetPosition(startPosition);

            this.powerupState = POWERUP_STATE_CLOSED;
            this.powerupType = this.game.random.RandomInt(POWERUP_TYPE_MAX + 1);
            if (this.powerupType >= POWERUP_TYPE_MAX) {
                this.powerupType = 0;
            }

            /*OutputDebugString(va("Powerup: %d\n", powerupType));
             if(powerupType == 0) {
             int x = 0;
             }*/
            SetMaterial(powerupMaterials[this.powerupType][this.powerupState]);
        }

        public static SSDPowerup GetNewPowerup(idGameSSDWindow _game, float _speed, float _rotation) {

            for (int i = 0; i < MAX_POWERUPS; i++) {
                if (!powerupPool[i].inUse) {
                    powerupPool[i].Init(_game, _speed, _rotation);
                    powerupPool[i].inUse = true;
                    return powerupPool[i];
                }
            }
            return null;
        }

        public static SSDPowerup GetSpecificPowerup(int id) {
            return powerupPool[id];
        }

        public static void WritePowerups(idFile savefile) {
            int count = 0;
            for (int i = 0; i < MAX_POWERUPS; i++) {
                if (powerupPool[i].inUse) {
                    count++;
                }
            }
            savefile.WriteInt(count);
            for (int i = 0; i < MAX_POWERUPS; i++) {
                if (powerupPool[i].inUse) {
                    savefile.WriteInt(powerupPool[i].id);
                    powerupPool[i].WriteToSaveGame(savefile);
                }
            }
        }

        public static void ReadPowerups(idFile savefile, idGameSSDWindow _game) {

            int count;
            count = savefile.ReadInt();
            for (int i = 0; i < count; i++) {
                int id;
                id = savefile.ReadInt();
                final SSDPowerup ent = GetSpecificPowerup(id);
                ent.ReadFromSaveGame(savefile, _game);
            }
        }
//
        protected static final SSDPowerup[] powerupPool = new SSDPowerup[MAX_POWERUPS];
    }

    public static class SSDLevelData_t implements SERiAL {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		float spawnBuffer;
        int needToWin;

        @Override
        public ByteBuffer AllocBuffer() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void Read(ByteBuffer buffer) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public ByteBuffer Write() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    public static class SSDAsteroidData_t implements SERiAL {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		float speedMin, speedMax;
        float sizeMin, sizeMax;
        float rotateMin, rotateMax;
        int spawnMin, spawnMax;
        int asteroidHealth;
        int asteroidPoints;
        int asteroidDamage;

        @Override
        public ByteBuffer AllocBuffer() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void Read(ByteBuffer buffer) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public ByteBuffer Write() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    public static class SSDAstronautData_t implements SERiAL {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		float speedMin, speedMax;
        float rotateMin, rotateMax;
        int spawnMin, spawnMax;
        int health;
        int points;
        int penalty;

        @Override
        public ByteBuffer AllocBuffer() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void Read(ByteBuffer buffer) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public ByteBuffer Write() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    public static class SSDPowerupData_t implements SERiAL {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		float speedMin, speedMax;
        float rotateMin, rotateMax;
        int spawnMin, spawnMax;

        @Override
        public ByteBuffer AllocBuffer() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void Read(ByteBuffer buffer) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public ByteBuffer Write() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    public static class SSDWeaponData_t implements SERiAL {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		float speed;
        int damage;
        int size;

        @Override
        public ByteBuffer AllocBuffer() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void Read(ByteBuffer buffer) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public ByteBuffer Write() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    /**
     * SSDLevelStats_t Data that is used for each level. This data is reset each
     * new level.
     */
    public static class SSDLevelStats_t {

        int shotCount;
        int hitCount;
        int destroyedAsteroids;
        int nextAsteroidSpawnTime;
//
        int killedAstronauts;
        int savedAstronauts;
//
        //Astronaut Level Data
        int nextAstronautSpawnTime;
//
        //Powerup Level Data
        int nextPowerupSpawnTime;
//
        SSDEntity targetEnt;
    }

    /**
     * SSDGameStats_t Data that is used for the game that is currently running.
     * Memset this to completely reset the game
     */
    public static class SSDGameStats_t implements SERiAL {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		boolean gameRunning;
//
        int score;
        int prebonusscore;
//
        int health;
//
        int currentWeapon;
        int currentLevel;
        int nextLevel;
//
        SSDLevelStats_t levelStats;

        @Override
        public ByteBuffer AllocBuffer() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void Read(ByteBuffer buffer) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public ByteBuffer Write() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    /*
     *****************************************************************************
     * idGameSSDWindow
     ****************************************************************************
     */
    public static class idGameSSDWindow extends idWindow {

        //WinVars used to call functions from the guis
        public idWinBool beginLevel;
        public idWinBool resetGame;
        public idWinBool continueGame;
        public idWinBool refreshGuiData;
        //
        public SSDCrossHair crosshair;
        public idBounds screenBounds;
        //
        //Level Data
        public int levelCount;
        public idList<SSDLevelData_t> levelData;
        public idList<SSDAsteroidData_t> asteroidData;
        public idList<SSDAstronautData_t> astronautData;
        public idList<SSDPowerupData_t> powerupData;
        //
        //	
        //Weapon Data
        public int weaponCount;
        public idList<SSDWeaponData_t> weaponData;
        //
        public int superBlasterTimeout;
        //
        //All current game data is stored in this structure (except the entity list)
        public SSDGameStats_t gameStats;
        public idList<SSDEntity> entities;
        //
        public int currentSound;
        //
        //

        public idGameSSDWindow(idUserInterfaceLocal gui) {
            super(gui);
            this.gui = gui;
            CommonInit();
        }

        public idGameSSDWindow(idDeviceContext dc, idUserInterfaceLocal gui) {
            super(dc, gui);
            this.dc = dc;
            this.gui = gui;
            CommonInit();
        }
//	~idGameSSDWindow();

        @Override
        public void WriteToSaveGame(idFile savefile) {
            super.WriteToSaveGame(savefile);

            savefile.WriteInt(this.ssdTime);

            this.beginLevel.WriteToSaveGame(savefile);
            this.resetGame.WriteToSaveGame(savefile);
            this.continueGame.WriteToSaveGame(savefile);
            this.refreshGuiData.WriteToSaveGame(savefile);

            this.crosshair.WriteToSaveGame(savefile);
            savefile.Write(this.screenBounds);

            savefile.WriteInt(this.levelCount);
            for (int i = 0; i < this.levelCount; i++) {
                savefile.Write(this.levelData.oGet(i));
                savefile.Write(this.asteroidData.oGet(i));
                savefile.Write(this.astronautData.oGet(i));
                savefile.Write(this.powerupData.oGet(i));
            }

            savefile.WriteInt(this.weaponCount);
            for (int i = 0; i < this.weaponCount; i++) {
                savefile.Write(this.weaponData.oGet(i));
            }

            savefile.WriteInt(this.superBlasterTimeout);
            savefile.Write(this.gameStats);

            //Write All Static Entities
            SSDAsteroid.WriteAsteroids(savefile);
            SSDAstronaut.WriteAstronauts(savefile);
            SSDExplosion.WriteExplosions(savefile);
            SSDPoints.WritePoints(savefile);
            SSDProjectile.WriteProjectiles(savefile);
            SSDPowerup.WritePowerups(savefile);

            final int entCount = this.entities.Num();
            savefile.WriteInt(entCount);
            for (int i = 0; i < entCount; i++) {
                savefile.WriteInt(this.entities.oGet(i).type);
                savefile.WriteInt(this.entities.oGet(i).id);
            }
        }

        @Override
        public void ReadFromSaveGame(idFile savefile) {
            super.ReadFromSaveGame(savefile);

            this.ssdTime = savefile.ReadInt();

            this.beginLevel.ReadFromSaveGame(savefile);
            this.resetGame.ReadFromSaveGame(savefile);
            this.continueGame.ReadFromSaveGame(savefile);
            this.refreshGuiData.ReadFromSaveGame(savefile);

            this.crosshair.ReadFromSaveGame(savefile);
            savefile.Read(this.screenBounds);

            this.levelCount = savefile.ReadInt();
            for (int i = 0; i < this.levelCount; i++) {
                final SSDLevelData_t newLevel = new SSDLevelData_t();
                savefile.Read(newLevel);
                this.levelData.Append(newLevel);

                final SSDAsteroidData_t newAsteroid = new SSDAsteroidData_t();
                savefile.Read(newAsteroid);
                this.asteroidData.Append(newAsteroid);

                final SSDAstronautData_t newAstronaut = new SSDAstronautData_t();
                savefile.Read(newAstronaut);
                this.astronautData.Append(newAstronaut);

                final SSDPowerupData_t newPowerup = new SSDPowerupData_t();
                savefile.Read(newPowerup);
                this.powerupData.Append(newPowerup);
            }

            this.weaponCount = savefile.ReadInt();
            for (int i = 0; i < this.weaponCount; i++) {
                final SSDWeaponData_t newWeapon = new SSDWeaponData_t();
                savefile.Read(newWeapon);
                this.weaponData.Append(newWeapon);
            }

            this.superBlasterTimeout = savefile.ReadInt();

            savefile.Read(this.gameStats);
            //Reset this because it is no longer valid
            this.gameStats.levelStats.targetEnt = null;

            SSDAsteroid.ReadAsteroids(savefile, this);
            SSDAstronaut.ReadAstronauts(savefile, this);
            SSDExplosion.ReadExplosions(savefile, this);
            SSDPoints.ReadPoints(savefile, this);
            SSDProjectile.ReadProjectiles(savefile, this);
            SSDPowerup.ReadPowerups(savefile, this);

            int entCount;
            entCount = savefile.ReadInt();

            for (int i = 0; i < entCount; i++) {
                SSD type;
                int id;
                type = SSD.values()[savefile.ReadInt()];
                id = savefile.ReadInt();

                final SSDEntity ent = GetSpecificEntity(type, id);
                if (ent != null) {
                    this.entities.Append(ent);
                }
            }
        }

        @Override
        public String HandleEvent(final sysEvent_s event, boolean[] updateVisuals) {

            // need to call this to allow proper focus and capturing on embedded children
            final String ret = super.HandleEvent(event, updateVisuals);

            if (!this.gameStats.gameRunning) {
                return ret;
            }

            final int key = event.evValue;

            if (event.evType == SE_KEY) {

                if (0 == event.evValue2) {
                    return ret;
                }

                if ((key == K_MOUSE1) || (key == K_MOUSE2)) {
                    FireWeapon(key);
                } else {
                    return ret;
                }
            }
            return ret;
        }

        @Override
        public idWinVar GetWinVarByName(final String _name, boolean winLookup /*= false*/, drawWin_t[] owner /*= NULL*/) {

            idWinVar retVar = null;

            if (idStr.Icmp(_name, "beginLevel") == 0) {
                retVar = this.beginLevel;
            }

            if (idStr.Icmp(_name, "resetGame") == 0) {
                retVar = this.resetGame;
            }

            if (idStr.Icmp(_name, "continueGame") == 0) {
                retVar = this.continueGame;
            }
            if (idStr.Icmp(_name, "refreshGuiData") == 0) {
                retVar = this.refreshGuiData;
            }

            if (retVar != null) {
                return retVar;
            }

            return super.GetWinVarByName(_name, winLookup, owner);
        }

        @Override
        public void Draw(int time, float x, float y) {

            //Update the game every frame before drawing
            UpdateGame();

            RefreshGuiData();

            if (this.gameStats.gameRunning) {

                ZOrderEntities();

                //Draw from back to front
                for (int i = this.entities.Num() - 1; i >= 0; i--) {
                    this.entities.oGet(i).Draw(this.dc);
                }

                //The last thing to draw is the crosshair
                final idVec2 cursor = new idVec2();
                //GetCursor(cursor);
                cursor.x = this.gui.CursorX();
                cursor.y = this.gui.CursorY();

                this.crosshair.Draw(this.dc, cursor);
            }
        }

        public void AddHealth(int health) {
            this.gameStats.health += health;
            this.gameStats.health = Min(100, this.gameStats.health);
        }

        public void AddScore(SSDEntity ent, int points) {

            SSDPoints pointsEnt;

            if (points > 0) {
                pointsEnt = SSDPoints.GetNewPoints(this, ent, points, 1000, 50, new idVec4(0, 1, 0, 1));
            } else {
                pointsEnt = SSDPoints.GetNewPoints(this, ent, points, 1000, 50, new idVec4(1, 0, 0, 1));
            }
            this.entities.Append(pointsEnt);

            this.gameStats.score += points;
            this.gui.SetStateString("player_score", va("%d", this.gameStats.score));
        }

        public void AddDamage(int damage) {
            this.gameStats.health -= damage;
            this.gui.SetStateString("player_health", va("%d", this.gameStats.health));

            this.gui.HandleNamedEvent("playerDamage");

            if (this.gameStats.health <= 0) {
                //The player is dead
                GameOver();
            }
        }

        public void OnNuke() {

            this.gui.HandleNamedEvent("nuke");

            //Destory All Asteroids
            for (int i = 0; i < this.entities.Num(); i++) {

                if (this.entities.oGet(i).type == SSD_ENTITY_ASTEROID) {

                    //The asteroid has been destroyed
                    final SSDExplosion explosion = SSDExplosion.GetNewExplosion(this, this.entities.oGet(i).position, this.entities.oGet(i).size.oMultiply(2), 300, SSDExplosion.EXPLOSION_NORMAL, this.entities.oGet(i));
                    this.entities.Append(explosion);

                    AddScore(this.entities.oGet(i), this.asteroidData.oGet(this.gameStats.currentLevel).asteroidPoints);

                    //Don't let the player hit it anymore because 
                    this.entities.oGet(i).noHit = true;

                    this.gameStats.levelStats.destroyedAsteroids++;
                }
            }
            PlaySound("arcade_explode");

            //Check to see if a nuke ends the level
	/*if(gameStats.levelStats.destroyedAsteroids >= levelData[gameStats.currentLevel].needToWin) {
             LevelComplete();

             }*/
        }

        public void OnRescueAll() {

            this.gui.HandleNamedEvent("rescueAll");

            //Rescue All Astronauts
            for (int i = 0; i < this.entities.Num(); i++) {

                if (this.entities.oGet(i).type == SSD_ENTITY_ASTRONAUT) {

                    AstronautStruckPlayer((SSDAstronaut) this.entities.oGet(i));
                }
            }
        }

        public void OnSuperBlaster() {

            StartSuperBlaster();
        }

        public SSDEntity GetSpecificEntity(SSD type, int id) {
            SSDEntity ent = null;
            switch (type) {
                case SSD_ENTITY_ASTEROID:
                    ent = SSDAsteroid.GetSpecificAsteroid(id);
                    break;
                case SSD_ENTITY_ASTRONAUT:
                    ent = SSDAstronaut.GetSpecificAstronaut(id);
                    break;
                case SSD_ENTITY_EXPLOSION:
                    ent = SSDExplosion.GetSpecificExplosion(id);
                    break;
                case SSD_ENTITY_POINTS:
                    ent = SSDPoints.GetSpecificPoints(id);
                    break;
                case SSD_ENTITY_PROJECTILE:
                    ent = SSDProjectile.GetSpecificProjectile(id);
                    break;
                case SSD_ENTITY_POWERUP:
                    ent = SSDPowerup.GetSpecificPowerup(id);
                    break;
			default:
				// TODO check unused Enum case labels
				break;
            }
            return ent;
        }
        static final int MAX_SOUND_CHANNEL = 8;

        public void PlaySound(final String sound) {

            session.sw.PlayShaderDirectly(sound, this.currentSound);

            this.currentSound++;
            if (this.currentSound >= MAX_SOUND_CHANNEL) {
                this.currentSound = 0;
            }
        }
//
        public static idRandom random;
        public int ssdTime;
//
        //Initialization

        @Override
        protected boolean ParseInternalVar(final String _name, idParser src) {

            if (idStr.Icmp(_name, "beginLevel") == 0) {
                this.beginLevel.oSet(src.ParseBool());
                return true;
            }
            if (idStr.Icmp(_name, "resetGame") == 0) {
                this.resetGame.oSet(src.ParseBool());
                return true;
            }
            if (idStr.Icmp(_name, "continueGame") == 0) {
                this.continueGame.oSet(src.ParseBool());
                return true;
            }
            if (idStr.Icmp(_name, "refreshGuiData") == 0) {
                this.refreshGuiData.oSet(src.ParseBool());
                return true;
            }

            if (idStr.Icmp(_name, "levelcount") == 0) {
                this.levelCount = src.ParseInt();
                for (int i = 0; i < this.levelCount; i++) {
                    final SSDLevelData_t newLevel = new SSDLevelData_t();
//                    memset(newLevel, 0, sizeof(SSDLevelData_t));
                    this.levelData.Append(newLevel);

                    final SSDAsteroidData_t newAsteroid = new SSDAsteroidData_t();
//                    memset(newAsteroid, 0, sizeof(SSDAsteroidData_t));
                    this.asteroidData.Append(newAsteroid);

                    final SSDAstronautData_t newAstronaut = new SSDAstronautData_t();
//                    memset(newAstronaut, 0, sizeof(SSDAstronautData_t));
                    this.astronautData.Append(newAstronaut);

                    final SSDPowerupData_t newPowerup = new SSDPowerupData_t();
//                    memset(newPowerup, 0, sizeof(SSDPowerupData_t));
                    this.powerupData.Append(newPowerup);

                }
                return true;
            }
            if (idStr.Icmp(_name, "weaponCount") == 0) {
                this.weaponCount = src.ParseInt();
                for (int i = 0; i < this.weaponCount; i++) {
                    final SSDWeaponData_t newWeapon = new SSDWeaponData_t();
//                    memset(newWeapon, 0, sizeof(SSDWeaponData_t));
                    this.weaponData.Append(newWeapon);
                }
                return true;
            }

            if (idStr.FindText(_name, "leveldata", false) >= 0) {
                final idStr tempName = new idStr(_name);
                final int level = atoi(tempName.Right(2)) - 1;

                final idStr levelData = new idStr();
                ParseString(src, levelData);
                ParseLevelData(level, levelData);
                return true;
            }

            if (idStr.FindText(_name, "asteroiddata", false) >= 0) {
                final idStr tempName = new idStr(_name);
                final int level = atoi(tempName.Right(2)) - 1;

                final idStr asteroidData = new idStr();
                ParseString(src, asteroidData);
                ParseAsteroidData(level, asteroidData);
                return true;
            }

            if (idStr.FindText(_name, "weapondata", false) >= 0) {
                final idStr tempName = new idStr(_name);
                final int weapon = atoi(tempName.Right(2)) - 1;

                final idStr weaponData = new idStr();
                ParseString(src, weaponData);
                ParseWeaponData(weapon, weaponData);
                return true;
            }

            if (idStr.FindText(_name, "astronautdata", false) >= 0) {
                final idStr tempName = new idStr(_name);
                final int level = atoi(tempName.Right(2)) - 1;

                final idStr astronautData = new idStr();
                ParseString(src, astronautData);
                ParseAstronautData(level, astronautData);
                return true;
            }

            if (idStr.FindText(_name, "powerupdata", false) >= 0) {
                final idStr tempName = new idStr(_name);
                final int level = atoi(tempName.Right(2)) - 1;

                final idStr powerupData = new idStr();
                ParseString(src, powerupData);
                ParsePowerupData(level, powerupData);
                return true;
            }

            return super.ParseInternalVar(_name, src);
        }

        private void ParseLevelData(int level, final idStr levelDataString) {

            final idParser parser = new idParser();
            final idToken token;
            parser.LoadMemory(levelDataString.getData(), levelDataString.Length(), "LevelData");

            this.levelData.oGet(level).spawnBuffer = parser.ParseFloat();
            this.levelData.oGet(level).needToWin = parser.ParseInt(); //Required Destroyed

        }

        private void ParseAsteroidData(int level, final idStr asteroidDataString) {

            final idParser parser = new idParser();
            final idToken token;
            parser.LoadMemory(asteroidDataString.getData(), asteroidDataString.Length(), "AsteroidData");

            this.asteroidData.oGet(level).speedMin = parser.ParseFloat(); //Speed Min 
            this.asteroidData.oGet(level).speedMax = parser.ParseFloat(); //Speed Max

            this.asteroidData.oGet(level).sizeMin = parser.ParseFloat(); //Size Min 
            this.asteroidData.oGet(level).sizeMax = parser.ParseFloat(); //Size Max

            this.asteroidData.oGet(level).rotateMin = parser.ParseFloat(); //Rotate Min (rotations per second) 
            this.asteroidData.oGet(level).rotateMax = parser.ParseFloat(); //Rotate Max (rotations per second)

            this.asteroidData.oGet(level).spawnMin = parser.ParseInt(); //Spawn Min
            this.asteroidData.oGet(level).spawnMax = parser.ParseInt(); //Spawn Max

            this.asteroidData.oGet(level).asteroidHealth = parser.ParseInt(); //Health of the asteroid
            this.asteroidData.oGet(level).asteroidDamage = parser.ParseInt(); //Asteroid Damage
            this.asteroidData.oGet(level).asteroidPoints = parser.ParseInt(); //Points awarded for destruction
        }

        private void ParseWeaponData(int weapon, final idStr weaponDataString) {

            final idParser parser = new idParser();
            final idToken token;
            parser.LoadMemory(weaponDataString.getData(), weaponDataString.Length(), "WeaponData");

            this.weaponData.oGet(weapon).speed = parser.ParseFloat();
            this.weaponData.oGet(weapon).damage = (int) parser.ParseFloat();
            this.weaponData.oGet(weapon).size = (int) parser.ParseFloat();
        }

        private void ParseAstronautData(int level, final idStr astronautDataString) {

            final idParser parser = new idParser();
            final idToken token;
            parser.LoadMemory(astronautDataString.getData(), astronautDataString.Length(), "AstronautData");

            this.astronautData.oGet(level).speedMin = parser.ParseFloat(); //Speed Min 
            this.astronautData.oGet(level).speedMax = parser.ParseFloat(); //Speed Max

            this.astronautData.oGet(level).rotateMin = parser.ParseFloat(); //Rotate Min (rotations per second) 
            this.astronautData.oGet(level).rotateMax = parser.ParseFloat(); //Rotate Max (rotations per second)

            this.astronautData.oGet(level).spawnMin = parser.ParseInt(); //Spawn Min
            this.astronautData.oGet(level).spawnMax = parser.ParseInt(); //Spawn Max

            this.astronautData.oGet(level).health = parser.ParseInt(); //Health of the asteroid
            this.astronautData.oGet(level).points = parser.ParseInt(); //Asteroid Damage
            this.astronautData.oGet(level).penalty = parser.ParseInt(); //Points awarded for destruction
        }

        private void ParsePowerupData(int level, final idStr powerupDataString) {

            final idParser parser = new idParser();
            final idToken token;
            parser.LoadMemory(powerupDataString.getData(), powerupDataString.Length(), "PowerupData");

            this.powerupData.oGet(level).speedMin = parser.ParseFloat(); //Speed Min 
            this.powerupData.oGet(level).speedMax = parser.ParseFloat(); //Speed Max

            this.powerupData.oGet(level).rotateMin = parser.ParseFloat(); //Rotate Min (rotations per second) 
            this.powerupData.oGet(level).rotateMax = parser.ParseFloat(); //Rotate Max (rotations per second)

            this.powerupData.oGet(level).spawnMin = parser.ParseInt(); //Spawn Min
            this.powerupData.oGet(level).spawnMax = parser.ParseInt(); //Spawn Max

        }

        private void CommonInit() {
            this.crosshair.InitCrosshairs();

            this.beginLevel.data = false;
            this.resetGame.data = false;
            this.continueGame.data = false;
            this.refreshGuiData.data = false;

            this.ssdTime = 0;
            this.levelCount = 0;
            this.weaponCount = 0;
            this.screenBounds = new idBounds(new idVec3(-320, -240, 0), new idVec3(320, 240, 0));

            this.superBlasterTimeout = 0;

            this.currentSound = 0;

            //Precahce all assets that are loaded dynamically
            declManager.FindMaterial(ASTEROID_MATERIAL);
            declManager.FindMaterial(ASTRONAUT_MATERIAL);

            for (int i = 0; i < EXPLOSION_MATERIAL_COUNT; i++) {
                declManager.FindMaterial(explosionMaterials[i]);
            }
            declManager.FindMaterial(PROJECTILE_MATERIAL);
            for (int i = 0; i < POWERUP_MATERIAL_COUNT; i++) {
                declManager.FindMaterial(powerupMaterials[i][0]);
                declManager.FindMaterial(powerupMaterials[i][1]);
            }

            // Precache sounds
            declManager.FindSound("arcade_blaster");
            declManager.FindSound("arcade_capture ");
            declManager.FindSound("arcade_explode");

            ResetGameStats();
        }

        private void ResetGameStats() {

            ResetEntities();

            //Reset the gamestats structure
//            memset(gameStats, 0);
            this.gameStats = new SSDGameStats_t();

            this.gameStats.health = 100;
        }

        private void ResetLevelStats() {

            ResetEntities();

            //Reset the level statistics structure
//            memset(gameStats.levelStats, 0, sizeof(gameStats.levelStats));
            this.gameStats.levelStats = new SSDLevelStats_t();
        }

        private void ResetEntities() {
            //Destroy all of the entities
            for (int i = 0; i < this.entities.Num(); i++) {
                this.entities.oGet(i).DestroyEntity();
            }
            this.entities.Clear();
        }

        //Game Running Methods
        private void StartGame() {

            this.gameStats.gameRunning = true;
        }

        private void StopGame() {

            this.gameStats.gameRunning = false;
        }

        private void GameOver() {

            StopGame();

            this.gui.HandleNamedEvent("gameOver");
        }

        //Starting the Game
        private void BeginLevel(int level) {

            ResetLevelStats();

            this.gameStats.currentLevel = level;

            StartGame();
        }

        /**
         * Continue game resets the players health
         */
        private void ContinueGame() {
            this.gameStats.health = 100;

            StartGame();
        }

        //Stopping the Game
        private void LevelComplete() {

            this.gameStats.prebonusscore = this.gameStats.score;

            // Add the bonuses
            int accuracy;
            if (0 == this.gameStats.levelStats.shotCount) {
                accuracy = 0;
            } else {
                accuracy = (int) (((float) this.gameStats.levelStats.hitCount / (float) this.gameStats.levelStats.shotCount) * 100.0f);
            }
            int accuracyPoints = Max(0, accuracy - 50) * 20;

            this.gui.SetStateString("player_accuracy_score", va("%d", accuracyPoints));

            this.gameStats.score += accuracyPoints;

            int saveAccuracy;
            final int totalAst = this.gameStats.levelStats.savedAstronauts + this.gameStats.levelStats.killedAstronauts;
            if (0 == totalAst) {
                saveAccuracy = 0;
            } else {
                saveAccuracy = (int) (((float) this.gameStats.levelStats.savedAstronauts / (float) totalAst) * 100.0f);
            }
            accuracyPoints = Max(0, saveAccuracy - 50) * 20;

            this.gui.SetStateString("save_accuracy_score", va("%d", accuracyPoints));

            this.gameStats.score += accuracyPoints;

            StopSuperBlaster();

            this.gameStats.nextLevel++;

            if (this.gameStats.nextLevel >= this.levelCount) {
                //Have they beaten the game
                GameComplete();
            } else {

                //Make sure we don't go above the levelcount
                //min(gameStats.nextLevel, levelCount-1);
                StopGame();
                this.gui.HandleNamedEvent("levelComplete");
            }
        }

        private void GameComplete() {
            StopGame();
            this.gui.HandleNamedEvent("gameComplete");
        }

        private void UpdateGame() {

            //Check to see if and functions where called by the gui
            if (this.beginLevel.data == true) {
                this.beginLevel.data = false;
                BeginLevel(this.gameStats.nextLevel);
            }
            if (this.resetGame.data == true) {
                this.resetGame.data = false;
                ResetGameStats();
            }
            if (this.continueGame.data == true) {
                this.continueGame.data = false;
                ContinueGame();
            }
            if (this.refreshGuiData.data == true) {
                this.refreshGuiData.data = false;
                RefreshGuiData();
            }

            if (this.gameStats.gameRunning) {

                //We assume an upate every 16 milliseconds
                this.ssdTime += 16;

                if ((this.superBlasterTimeout != 0) && (this.ssdTime > this.superBlasterTimeout)) {
                    StopSuperBlaster();
                }

                //Find if we are targeting and enemy
                final idVec2 cursor = new idVec2();
                //GetCursor(cursor);
                cursor.x = this.gui.CursorX();
                cursor.y = this.gui.CursorY();
                this.gameStats.levelStats.targetEnt = EntityHitTest(cursor);

                //Update from back to front
                for (int i = this.entities.Num() - 1; i >= 0; i--) {
                    this.entities.oGet(i).Update();
                }

                CheckForHits();

                //Delete entities that need to be deleted
                for (int i = this.entities.Num() - 1; i >= 0; i--) {
                    if (this.entities.oGet(i).destroyed) {
                        final SSDEntity ent = this.entities.oGet(i);
                        ent.DestroyEntity();
                        this.entities.RemoveIndex(i);
                    }
                }

                //Check if we can spawn an asteroid
                SpawnAsteroid();

                //Check if we should spawn an astronaut
                SpawnAstronaut();

                //Check if we should spawn an asteroid
                SpawnPowerup();
            }
        }

        private void CheckForHits() {

            //See if the entity has gotten close enough
            for (int i = 0; i < this.entities.Num(); i++) {
                final SSDEntity ent = this.entities.oGet(i);
                if (ent.position.z <= Z_NEAR) {

                    if (!ent.noPlayerDamage) {

                        //Is the object still in the screen
                        final idVec3 entPos = ent.position;
                        entPos.z = 0;

                        final idBounds entBounds = new idBounds(entPos);
                        entBounds.ExpandSelf(ent.hitRadius);

                        if (this.screenBounds.IntersectsBounds(entBounds)) {

                            ent.OnStrikePlayer();

                            //The entity hit the player figure out what is was and act appropriately
                            if (ent.type == SSD_ENTITY_ASTEROID) {
                                AsteroidStruckPlayer((SSDAsteroid) ent);
                            } else if (ent.type == SSD_ENTITY_ASTRONAUT) {
                                AstronautStruckPlayer((SSDAstronaut) ent);
                            }
                        } else {
                            //Tag for removal later in the frame
                            ent.destroyed = true;
                        }
                    }
                }
            }
        }

        private void ZOrderEntities() {
            //Z-Order the entities
            //Using a simple sorting method
            for (int i = this.entities.Num() - 1; i >= 0; i--) {
                boolean flipped = false;
                for (int j = 0; j < i; j++) {
                    if (this.entities.oGet(j).position.z > this.entities.oGet(j + 1).position.z) {
                        final SSDEntity ent = this.entities.oGet(j);
                        this.entities.oSet(j, this.entities.oGet(j + 1));
                        this.entities.oSet(j + 1, ent);
                        flipped = true;
                    }
                }
                if (!flipped) {
                    //Jump out because it is sorted
                    break;
                }
            }
        }

        private void SpawnAsteroid() {

            final int currentTime = this.ssdTime;

            if (currentTime < this.gameStats.levelStats.nextAsteroidSpawnTime) {
                //Not time yet
                return;
            }

            //Lets spawn it
            final idVec3 startPosition = new idVec3();

            final float spawnBuffer = this.levelData.oGet(this.gameStats.currentLevel).spawnBuffer * 2.0f;
            startPosition.x = random.RandomInt(V_WIDTH + spawnBuffer) - ((V_WIDTH / 2.0f) + spawnBuffer);
            startPosition.y = random.RandomInt(V_HEIGHT + spawnBuffer) - ((V_HEIGHT / 2.0f) + spawnBuffer);
            startPosition.z = ENTITY_START_DIST;

            final float speed = random.RandomInt(this.asteroidData.oGet(this.gameStats.currentLevel).speedMax - this.asteroidData.oGet(this.gameStats.currentLevel).speedMin) + this.asteroidData.oGet(this.gameStats.currentLevel).speedMin;
            final float size = random.RandomInt(this.asteroidData.oGet(this.gameStats.currentLevel).sizeMax - this.asteroidData.oGet(this.gameStats.currentLevel).sizeMin) + this.asteroidData.oGet(this.gameStats.currentLevel).sizeMin;
            final float rotate = (random.RandomFloat() * (this.asteroidData.oGet(this.gameStats.currentLevel).rotateMax - this.asteroidData.oGet(this.gameStats.currentLevel).rotateMin)) + this.asteroidData.oGet(this.gameStats.currentLevel).rotateMin;

            final SSDAsteroid asteroid = SSDAsteroid.GetNewAsteroid(this, startPosition, new idVec2(size, size), speed, rotate, this.asteroidData.oGet(this.gameStats.currentLevel).asteroidHealth);
            this.entities.Append(asteroid);

            this.gameStats.levelStats.nextAsteroidSpawnTime = currentTime + random.RandomInt(this.asteroidData.oGet(this.gameStats.currentLevel).spawnMax - this.asteroidData.oGet(this.gameStats.currentLevel).spawnMin) + this.asteroidData.oGet(this.gameStats.currentLevel).spawnMin;
        }

        private void FireWeapon(int key) {

            final idVec2 cursorWorld = GetCursorWorld();
            final idVec2 cursor = new idVec2();
            //GetCursor(cursor);
            cursor.x = this.gui.CursorX();
            cursor.y = this.gui.CursorY();

            if (key == K_MOUSE1) {

                this.gameStats.levelStats.shotCount++;

                if (this.gameStats.levelStats.targetEnt != null) {
                    //Aim the projectile from the bottom of the screen directly at the ent
                    //SSDProjectile* newProj = new SSDProjectile(this, idVec3(320,0,0), gameStats.levelStats.targetEnt.position, weaponData[gameStats.currentWeapon].speed, weaponData[gameStats.currentWeapon].size);
                    final SSDProjectile newProj = SSDProjectile.GetNewProjectile(this, new idVec3(0, -180, 0), this.gameStats.levelStats.targetEnt.position, this.weaponData.oGet(this.gameStats.currentWeapon).speed, this.weaponData.oGet(this.gameStats.currentWeapon).size);
                    this.entities.Append(newProj);
                    //newProj = SSDProjectile::GetNewProjectile(this, idVec3(-320,-0,0), gameStats.levelStats.targetEnt.position, weaponData[gameStats.currentWeapon].speed, weaponData[gameStats.currentWeapon].size);
                    //entities.Append(newProj);

                    //We hit something
                    this.gameStats.levelStats.hitCount++;

                    this.gameStats.levelStats.targetEnt.OnHit(key);

                    if (this.gameStats.levelStats.targetEnt.type == SSD_ENTITY_ASTEROID) {
                        HitAsteroid((SSDAsteroid) this.gameStats.levelStats.targetEnt, key);
                    } else if (this.gameStats.levelStats.targetEnt.type == SSD_ENTITY_ASTRONAUT) {
                        HitAstronaut((SSDAstronaut) this.gameStats.levelStats.targetEnt, key);
                    } else if (this.gameStats.levelStats.targetEnt.type == SSD_ENTITY_ASTRONAUT) {
                    }
                } else {
                    ////Aim the projectile at the cursor position all the way to the far clipping
                    //SSDProjectile* newProj = SSDProjectile::GetNewProjectile(this, idVec3(0,-180,0), idVec3(cursorWorld.x, cursorWorld.y, (Z_FAR-Z_NEAR)/2.0f), weaponData[gameStats.currentWeapon].speed, weaponData[gameStats.currentWeapon].size);

                    //Aim the projectile so it crosses the cursor 1/4 of screen
                    final idVec3 vec = new idVec3(cursorWorld.x, cursorWorld.y, (Z_FAR - Z_NEAR) / 8.0f);
                    vec.oMulSet(8);
                    final SSDProjectile newProj = SSDProjectile.GetNewProjectile(this, new idVec3(0, -180, 0), vec, this.weaponData.oGet(this.gameStats.currentWeapon).speed, this.weaponData.oGet(this.gameStats.currentWeapon).size);
                    this.entities.Append(newProj);

                }

                //Play the blaster sound
                PlaySound("arcade_blaster");

            } /*else if (key == K_MOUSE2) {
             if(gameStats.levelStats.targetEnt) {
             if(gameStats.levelStats.targetEnt.type == SSD_ENTITY_ASTRONAUT) {
             HitAstronaut(static_cast<SSDAstronaut*>(gameStats.levelStats.targetEnt), key);
             }
             }
             }*/

        }

        private SSDEntity EntityHitTest(final idVec2 pt) {

            for (int i = 0; i < this.entities.Num(); i++) {
                //Since we ZOrder the entities every frame we can stop at the first entity we hit.
                //ToDo: Make sure this assumption is true
                if (this.entities.oGet(i).HitTest(pt)) {
                    return this.entities.oGet(i);
                }
            }
            return null;
        }

        private void HitAsteroid(SSDAsteroid asteroid, int key) {

            asteroid.health -= this.weaponData.oGet(this.gameStats.currentWeapon).damage;

            if (asteroid.health <= 0) {

                //The asteroid has been destroyed
                final SSDExplosion explosion = SSDExplosion.GetNewExplosion(this, asteroid.position, asteroid.size.oMultiply(2), 300, SSDExplosion.EXPLOSION_NORMAL, asteroid);
                this.entities.Append(explosion);
                PlaySound("arcade_explode");

                AddScore(asteroid, this.asteroidData.oGet(this.gameStats.currentLevel).asteroidPoints);

                //Don't let the player hit it anymore because 
                asteroid.noHit = true;

                this.gameStats.levelStats.destroyedAsteroids++;
                //if(gameStats.levelStats.destroyedAsteroids >= levelData[gameStats.currentLevel].needToWin) {
                //	LevelComplete();
                //}

            } else {
                //This was a damage hit so create a real small quick explosion
                final SSDExplosion explosion = SSDExplosion.GetNewExplosion(this, asteroid.position, asteroid.size.oDivide(2.0f), 200, SSDExplosion.EXPLOSION_NORMAL, asteroid, false, false);
                this.entities.Append(explosion);
            }
        }

        private void AsteroidStruckPlayer(SSDAsteroid asteroid) {

            asteroid.noPlayerDamage = true;
            asteroid.noHit = true;

            AddDamage(this.asteroidData.oGet(this.gameStats.currentLevel).asteroidDamage);

            final SSDExplosion explosion = SSDExplosion.GetNewExplosion(this, asteroid.position, asteroid.size.oMultiply(2), 300, SSDExplosion.EXPLOSION_NORMAL, asteroid);
            this.entities.Append(explosion);
            PlaySound("arcade_explode");
        }

        private void RefreshGuiData() {

            this.gui.SetStateString("nextLevel", va("%d", this.gameStats.nextLevel + 1));
            this.gui.SetStateString("currentLevel", va("%d", this.gameStats.currentLevel + 1));

            float accuracy;
            if (0 == this.gameStats.levelStats.shotCount) {
                accuracy = 0;
            } else {
                accuracy = ((float) this.gameStats.levelStats.hitCount / (float) this.gameStats.levelStats.shotCount) * 100.0f;
            }
            this.gui.SetStateString("player_accuracy", va("%d%%", (int) accuracy));

            float saveAccuracy;
            final int totalAst = this.gameStats.levelStats.savedAstronauts + this.gameStats.levelStats.killedAstronauts;

            if (0 == totalAst) {
                saveAccuracy = 0;
            } else {
                saveAccuracy = ((float) this.gameStats.levelStats.savedAstronauts / (float) totalAst) * 100.0f;
            }
            this.gui.SetStateString("save_accuracy", va("%d%%", (int) saveAccuracy));

            if (this.gameStats.levelStats.targetEnt != null) {
                int dist = (int) (this.gameStats.levelStats.targetEnt.position.z / 100.0f);
                dist *= 100;
                this.gui.SetStateString("target_info", va("%d meters", dist));
            } else {
                this.gui.SetStateString("target_info", "No Target");
            }

            this.gui.SetStateString("player_health", va("%d", this.gameStats.health));
            this.gui.SetStateString("player_score", va("%d", this.gameStats.score));
            this.gui.SetStateString("player_prebonusscore", va("%d", this.gameStats.prebonusscore));
            this.gui.SetStateString("level_complete", va("%d/%d", this.gameStats.levelStats.savedAstronauts, this.levelData.oGet(this.gameStats.currentLevel).needToWin));

            if (this.superBlasterTimeout != 0) {
                final float timeRemaining = (this.superBlasterTimeout - this.ssdTime) / 1000.0f;
                this.gui.SetStateString("super_blaster_time", va("%.2f", timeRemaining));
            }
        }

        private idVec2 GetCursorWorld() {

            final idVec2 cursor = new idVec2();
            //GetCursor(cursor);
            cursor.x = this.gui.CursorX();
            cursor.y = this.gui.CursorY();
            cursor.x = cursor.x - (0.5f * V_WIDTH);
            cursor.y = -(cursor.y - (0.5f * V_HEIGHT));
            return cursor;
        }

        //Astronaut Methods
        private void SpawnAstronaut() {

            final int currentTime = this.ssdTime;

            if (currentTime < this.gameStats.levelStats.nextAstronautSpawnTime) {
                //Not time yet
                return;
            }

            //Lets spawn it
            final idVec3 startPosition = new idVec3();

            startPosition.x = random.RandomInt(V_WIDTH) - (V_WIDTH / 2.0f);
            startPosition.y = random.RandomInt(V_HEIGHT) - (V_HEIGHT / 2.0f);
            startPosition.z = ENTITY_START_DIST;

            final float speed = random.RandomInt(this.astronautData.oGet(this.gameStats.currentLevel).speedMax - this.astronautData.oGet(this.gameStats.currentLevel).speedMin) + this.astronautData.oGet(this.gameStats.currentLevel).speedMin;
            final float rotate = (random.RandomFloat() * (this.astronautData.oGet(this.gameStats.currentLevel).rotateMax - this.astronautData.oGet(this.gameStats.currentLevel).rotateMin)) + this.astronautData.oGet(this.gameStats.currentLevel).rotateMin;

            final SSDAstronaut astronaut = SSDAstronaut.GetNewAstronaut(this, startPosition, speed, rotate, this.astronautData.oGet(this.gameStats.currentLevel).health);
            this.entities.Append(astronaut);

            this.gameStats.levelStats.nextAstronautSpawnTime = currentTime + random.RandomInt(this.astronautData.oGet(this.gameStats.currentLevel).spawnMax - this.astronautData.oGet(this.gameStats.currentLevel).spawnMin) + this.astronautData.oGet(this.gameStats.currentLevel).spawnMin;
        }

        private void HitAstronaut(SSDAstronaut astronaut, int key) {

            if (key == K_MOUSE1) {
                astronaut.health -= this.weaponData.oGet(this.gameStats.currentWeapon).damage;

                if (astronaut.health <= 0) {

                    this.gameStats.levelStats.killedAstronauts++;

                    //The astronaut has been destroyed
                    final SSDExplosion explosion = SSDExplosion.GetNewExplosion(this, astronaut.position, astronaut.size.oMultiply(2), 300, SSDExplosion.EXPLOSION_NORMAL, astronaut);
                    this.entities.Append(explosion);
                    PlaySound("arcade_explode");

                    //Add the penalty for killing the astronaut
                    AddScore(astronaut, this.astronautData.oGet(this.gameStats.currentLevel).penalty);

                    //Don't let the player hit it anymore
                    astronaut.noHit = true;
                } else {
                    //This was a damage hit so create a real small quick explosion
                    final SSDExplosion explosion = SSDExplosion.GetNewExplosion(this, astronaut.position, astronaut.size.oDivide(2.0f), 200, SSDExplosion.EXPLOSION_NORMAL, astronaut, false, false);
                    this.entities.Append(explosion);
                }
            }
        }

        private void AstronautStruckPlayer(SSDAstronaut astronaut) {

            this.gameStats.levelStats.savedAstronauts++;

            astronaut.noPlayerDamage = true;
            astronaut.noHit = true;

            //We are saving an astronaut
            final SSDExplosion explosion = SSDExplosion.GetNewExplosion(this, astronaut.position, astronaut.size.oMultiply(2), 300, SSDExplosion.EXPLOSION_TELEPORT, astronaut);
            this.entities.Append(explosion);
            PlaySound("arcade_capture");

            //Give the player points for saving the astronaut
            AddScore(astronaut, this.astronautData.oGet(this.gameStats.currentLevel).points);

            if (this.gameStats.levelStats.savedAstronauts >= this.levelData.oGet(this.gameStats.currentLevel).needToWin) {
                LevelComplete();
            }

        }
        //Powerup Methods

        private void SpawnPowerup() {

            final int currentTime = this.ssdTime;

            if (currentTime < this.gameStats.levelStats.nextPowerupSpawnTime) {
                //Not time yet
                return;
            }

            final float speed = random.RandomInt(this.powerupData.oGet(this.gameStats.currentLevel).speedMax - this.powerupData.oGet(this.gameStats.currentLevel).speedMin) + this.powerupData.oGet(this.gameStats.currentLevel).speedMin;
            final float rotate = (random.RandomFloat() * (this.powerupData.oGet(this.gameStats.currentLevel).rotateMax - this.powerupData.oGet(this.gameStats.currentLevel).rotateMin)) + this.powerupData.oGet(this.gameStats.currentLevel).rotateMin;

            final SSDPowerup powerup = SSDPowerup.GetNewPowerup(this, speed, rotate);
            this.entities.Append(powerup);

            this.gameStats.levelStats.nextPowerupSpawnTime = currentTime + random.RandomInt(this.powerupData.oGet(this.gameStats.currentLevel).spawnMax - this.powerupData.oGet(this.gameStats.currentLevel).spawnMin) + this.powerupData.oGet(this.gameStats.currentLevel).spawnMin;
        }

        private void StartSuperBlaster() {

            this.gui.HandleNamedEvent("startSuperBlaster");
            this.gameStats.currentWeapon = 1;
            this.superBlasterTimeout = this.ssdTime + 10000;
        }

        private void StopSuperBlaster() {
            this.gui.HandleNamedEvent("stopSuperBlaster");
            this.gameStats.currentWeapon = 0;
            this.superBlasterTimeout = 0;
        }
        // void FreeSoundEmitter(bool immediate);
    }
}
