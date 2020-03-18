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

            savefile.WriteInt(currentCrosshair);
            savefile.WriteFloat(crosshairWidth);
            savefile.WriteFloat(crosshairHeight);

        }

        public void ReadFromSaveGame(idFile savefile) {

            InitCrosshairs();

            currentCrosshair = savefile.ReadInt();
            crosshairWidth = savefile.ReadFloat();
            crosshairHeight = savefile.ReadFloat();

        }

        public void InitCrosshairs() {

            crosshairMaterial[CROSSHAIR_STANDARD] = declManager.FindMaterial(CROSSHAIR_STANDARD_MATERIAL);
            crosshairMaterial[CROSSHAIR_SUPER] = declManager.FindMaterial(CROSSHAIR_SUPER_MATERIAL);

            crosshairWidth = 64;
            crosshairHeight = 64;

            currentCrosshair = CROSSHAIR_STANDARD;

        }

        public void Draw(idDeviceContext dc, final idVec2 cursor) {

            dc.DrawMaterial(cursor.x - (crosshairWidth / 2), cursor.y - (crosshairHeight / 2),
                    crosshairWidth, crosshairHeight,
                    crosshairMaterial[currentCrosshair], colorWhite, 1.0f, 1.0f);
        }
    };

    public enum SSD {

        SSD_ENTITY_BASE,//= 0,
        SSD_ENTITY_ASTEROID,
        SSD_ENTITY_ASTRONAUT,
        SSD_ENTITY_EXPLOSION,
        SSD_ENTITY_POINTS,
        SSD_ENTITY_PROJECTILE,
        SSD_ENTITY_POWERUP
    };

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

            savefile.WriteInt(type);
            game.WriteSaveGameString(materialName, savefile);
            savefile.Write(position);
            savefile.Write(size);
            savefile.WriteFloat(radius);
            savefile.WriteFloat(hitRadius);
            savefile.WriteFloat(rotation);

            savefile.Write(matColor);

            game.WriteSaveGameString(text, savefile);
            savefile.WriteFloat(textScale);
            savefile.Write(foreColor);

            savefile.WriteInt(currentTime);
            savefile.WriteInt(lastUpdate);
            savefile.WriteInt(elapsed);

            savefile.WriteBool(destroyed);
            savefile.WriteBool(noHit);
            savefile.WriteBool(noPlayerDamage);

            savefile.WriteBool(inUse);

        }

        public void ReadFromSaveGame(idFile savefile, idGameSSDWindow _game) {

            type = SSD.values()[savefile.ReadInt()];
            game.ReadSaveGameString(materialName, savefile);
            SetMaterial(materialName.toString());
            savefile.Read(position);
            savefile.Read(size);
            radius = savefile.ReadFloat();
            hitRadius = savefile.ReadFloat();
            rotation = savefile.ReadFloat();

            savefile.Read(matColor);

            game.ReadSaveGameString(text, savefile);
            textScale = savefile.ReadFloat();
            savefile.Read(foreColor);

            game = _game;
            currentTime = savefile.ReadInt();
            lastUpdate = savefile.ReadInt();
            elapsed = savefile.ReadInt();

            destroyed = savefile.ReadBool();
            noHit = savefile.ReadBool();
            noPlayerDamage = savefile.ReadBool();

            inUse = savefile.ReadBool();
        }

        public void EntityInit() {

            inUse = false;

            type = SSD_ENTITY_BASE;

            materialName = new idStr("");
            material = null;
            position.Zero();
            size.Zero();
            radius = 0.0f;
            hitRadius = 0.0f;
            rotation = 0.0f;

            currentTime = 0;
            lastUpdate = 0;

            destroyed = false;
            noHit = false;
            noPlayerDamage = false;

            matColor.Set(1, 1, 1, 1);

            text = new idStr("");
            textScale = 1.0f;
            foreColor.Set(1, 1, 1, 1);
        }

        public void SetGame(idGameSSDWindow _game) {
            game = _game;
        }

        public void SetMaterial(final String _name) {
            materialName.oSet(_name);
            material = declManager.FindMaterial(_name);
            material.SetSort(SS_GUI);
        }

        public void SetPosition(final idVec3 _position) {
            position = _position;//TODO:is this by value, or by reference?
        }

        public void SetSize(final idVec2 _size) {
            size = _size;
        }

        public void SetRadius(float _radius, float _hitFactor /*= 1.0f*/) {
            radius = _radius;
            hitRadius = _radius * _hitFactor;
        }

        public void SetRotation(float _rotation) {
            rotation = _rotation;
        }

        public void Update() {

            currentTime = game.ssdTime;

            //Is this the first update
            if (lastUpdate == 0) {
                lastUpdate = currentTime;
                return;
            }

            elapsed = currentTime - lastUpdate;

            EntityUpdate();

            lastUpdate = currentTime;
        }

        public boolean HitTest(final idVec2 pt) {

            if (noHit) {
                return false;
            }

            idVec3 screenPos = WorldToScreen(position);

            //Scale the radius based on the distance from the player
            float scale = 1.0f - ((screenPos.z - Z_NEAR) / (Z_FAR - Z_NEAR));
            float scaledRad = scale * hitRadius;

            //So we can compare against the square of the length between two points
            float scaleRadSqr = scaledRad * scaledRad;

            idVec2 diff = screenPos.ToVec2().oMinus(pt);
            float dist = idMath.Fabs(diff.LengthSqr());

            if (dist < scaleRadSqr) {
                return true;
            }
            return false;
        }

        public void EntityUpdate() {
        }

        public void Draw(idDeviceContext dc) {

            idVec2 persize = new idVec2();
            float x, y;

            idBounds bounds = new idBounds();
            bounds.oSet(0, new idVec3(position.x - (size.x / 2.0f), position.y - (size.y / 2.0f), position.z));
            bounds.oSet(1, new idVec3(position.x + (size.x / 2.0f), position.y + (size.y / 2.0f), position.z));

            idBounds screenBounds = WorldToScreen(bounds);
            persize.x = idMath.Fabs(screenBounds.oGet(1).x - screenBounds.oGet(0).x);
            persize.y = idMath.Fabs(screenBounds.oGet(1).y - screenBounds.oGet(0).y);

//	idVec3 center = screenBounds.GetCenter();
            x = screenBounds.oGet(0).x;
            y = screenBounds.oGet(1).y;
            dc.DrawMaterialRotated(x, y, persize.x, persize.y, material, matColor, 1.0f, 1.0f, (float) DEG2RAD(rotation));

            if (text.Length() > 0) {
                idRectangle rect = new idRectangle(x, y, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
                dc.DrawText(text.toString(), textScale, 0, foreColor, rect, false);
            }

        }

        public void DestroyEntity() {
            inUse = false;
        }

        public void OnHit(int key) {
        }

        public void OnStrikePlayer() {
        }

        public idBounds WorldToScreen(final idBounds worldBounds) {

            idVec3 screenMin = WorldToScreen(worldBounds.oGet(0));
            idVec3 screenMax = WorldToScreen(worldBounds.oGet(1));

            idBounds screenBounds = new idBounds(screenMin, screenMax);
            return screenBounds;
        }

        public idVec3 WorldToScreen(final idVec3 worldPos) {

            float d = 0.5f * V_WIDTH * idMath.Tan((float) (DEG2RAD(90.0f) / 2.0f));

            //World To Camera Coordinates
            idVec3 cameraTrans = new idVec3(0, 0, d);
            idVec3 cameraPos;
            cameraPos = worldPos.oPlus(cameraTrans);

            //Camera To Screen Coordinates
            idVec3 screenPos = new idVec3();
            screenPos.x = d * cameraPos.x / cameraPos.z + (0.5f * V_WIDTH - 0.5f);
            screenPos.y = -d * cameraPos.y / cameraPos.z + (0.5f * V_HEIGHT - 0.5f);
            screenPos.z = cameraPos.z;

            return screenPos;
        }

        public idVec3 ScreenToWorld(final idVec3 screenPos) {

            idVec3 worldPos = new idVec3();

            worldPos.x = screenPos.x - 0.5f * V_WIDTH;
            worldPos.y = -(screenPos.y - 0.5f * V_HEIGHT);
            worldPos.z = screenPos.z;

            return worldPos;
        }
    };

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

            savefile.Write(speed);
            savefile.WriteFloat(rotationSpeed);
        }

        @Override
        public void ReadFromSaveGame(idFile savefile, idGameSSDWindow _game) {
            super.ReadFromSaveGame(savefile, _game);

            savefile.Read(speed);
            rotationSpeed = savefile.ReadFloat();
        }

        public void MoverInit(final idVec3 _speed, float _rotationSpeed) {

            speed = _speed;
            rotationSpeed = _rotationSpeed;
        }

        @Override
        public void EntityUpdate() {

            super.EntityUpdate();

            //Move forward based on speed (units per second)
            idVec3 moved = speed.oMultiply((float) elapsed / 1000.0f);
            position.oPluSet(moved);

            float rotated = ((float) elapsed / 1000.0f) * rotationSpeed * 360.0f;
            rotation += rotated;
            if (rotation >= 360) {
                rotation -= 360.0f;
            }
            if (rotation < 0) {
                rotation += 360.0f;
            }
        }
    };
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

            savefile.WriteInt(health);
        }

        @Override
        public void ReadFromSaveGame(idFile savefile, idGameSSDWindow _game) {
            super.ReadFromSaveGame(savefile, _game);

            health = savefile.ReadInt();
        }

        public void Init(idGameSSDWindow _game, final idVec3 startPosition, final idVec2 _size, float _speed, float rotate, int _health) {

            EntityInit();
            MoverInit(new idVec3(0, 0, -_speed), rotate);

            SetGame(_game);

            type = SSD_ENTITY_ASTEROID;

            SetMaterial(ASTEROID_MATERIAL);
            SetSize(_size);
            SetRadius((float) Max(size.x, size.y), 0.3f);
            SetRotation(game.random.RandomInt(360));

            position = startPosition;

            health = _health;
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
                SSDAsteroid ent = GetSpecificAsteroid(id);
                ent.ReadFromSaveGame(savefile, _game);
            }
        }
//        
        protected static final SSDAsteroid[] asteroidPool = new SSDAsteroid[MAX_ASTEROIDS];
    };
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

            savefile.WriteInt(health);
        }

        @Override
        public void ReadFromSaveGame(idFile savefile, idGameSSDWindow _game) {
            super.ReadFromSaveGame(savefile, _game);

            health = savefile.ReadInt();
        }

        public void Init(idGameSSDWindow _game, final idVec3 startPosition, float _speed, float rotate, int _health) {

            EntityInit();
            MoverInit(new idVec3(0, 0, -_speed), rotate);

            SetGame(_game);

            type = SSD_ENTITY_ASTRONAUT;

            SetMaterial(ASTRONAUT_MATERIAL);
            SetSize(new idVec2(256, 256));
            SetRadius((float) Max(size.x, size.y), 0.3f);
            SetRotation(game.random.RandomInt(360));

            position = startPosition;
            health = _health;
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
                SSDAstronaut ent = GetSpecificAstronaut(id);
                ent.ReadFromSaveGame(savefile, _game);
            }
        }
//        
        protected static final SSDAstronaut[] astronautPool = new SSDAstronaut[MAX_ASTRONAUT];
    };
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
            type = SSD_ENTITY_EXPLOSION;
        }
        // ~SSDExplosion();

        @Override
        public void WriteToSaveGame(idFile savefile) {
            super.WriteToSaveGame(savefile);

            savefile.Write(finalSize);
            savefile.WriteInt(length);
            savefile.WriteInt(beginTime);
            savefile.WriteInt(endTime);
            savefile.WriteInt(explosionType);

            savefile.WriteInt(buddy.type);
            savefile.WriteInt(buddy.id);

            savefile.WriteBool(killBuddy);
            savefile.WriteBool(followBuddy);
        }

        @Override
        public void ReadFromSaveGame(idFile savefile, idGameSSDWindow _game) {
            super.ReadFromSaveGame(savefile, _game);

            savefile.Read(finalSize);
            length = savefile.ReadInt();
            beginTime = savefile.ReadInt();
            endTime = savefile.ReadInt();
            explosionType = savefile.ReadInt();

            SSD type;
            int id;
            type = SSD.values()[savefile.ReadInt()];
            id = savefile.ReadInt();

            //Get a pointer to my buddy
            buddy = _game.GetSpecificEntity(type, id);

            killBuddy = savefile.ReadBool();
            followBuddy = savefile.ReadBool();
        }

        public void Init(idGameSSDWindow _game, final idVec3 _position, final idVec2 _size, int _length, int _type, SSDEntity _buddy, boolean _killBuddy /*= true*/, boolean _followBuddy /*= true*/) {

            EntityInit();

            SetGame(_game);

            type = SSD_ENTITY_EXPLOSION;
            explosionType = _type;

            SetMaterial(explosionMaterials[explosionType]);
            SetPosition(_position);
            position.z -= 50;

            finalSize = _size;
            length = _length;
            beginTime = game.ssdTime;
            endTime = beginTime + length;

            buddy = _buddy;
            killBuddy = _killBuddy;
            followBuddy = _followBuddy;

            //Explosion Starts from nothing and will increase in size until it gets to final size
            size.Zero();

            noPlayerDamage = true;
            noHit = true;
        }

        @Override
        public void EntityUpdate() {

            super.EntityUpdate();

            //Always set my position to my buddies position except change z to be on top
            if (followBuddy) {
                position = buddy.position;
                position.z -= 50;
            } else {
                //Only mess with the z if we are not following
                position.z = buddy.position.z - 50;
            }

            //Scale the image based on the time
            size = finalSize.oMultiply((currentTime - beginTime) / length);

            //Destroy myself after the explosion is done
            if (currentTime > endTime) {
                destroyed = true;

                if (killBuddy) {
                    //Destroy the exploding object
                    buddy.destroyed = true;
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
                SSDExplosion ent = GetSpecificExplosion(id);
                ent.ReadFromSaveGame(savefile, _game);
            }
        }
//        
        protected static final SSDExplosion[] explosionPool = new SSDExplosion[MAX_EXPLOSIONS];
    };
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
            type = SSD_ENTITY_POINTS;
        }
        // ~SSDPoints();

        @Override
        public void WriteToSaveGame(idFile savefile) {
            super.WriteToSaveGame(savefile);

            savefile.WriteInt(length);
            savefile.WriteInt(distance);
            savefile.WriteInt(beginTime);
            savefile.WriteInt(endTime);

            savefile.Write(beginPosition);
            savefile.Write(endPosition);

            savefile.Write(beginColor);
            savefile.Write(endColor);

        }

        @Override
        public void ReadFromSaveGame(idFile savefile, idGameSSDWindow _game) {
            super.ReadFromSaveGame(savefile, _game);

            length = savefile.ReadInt();
            distance = savefile.ReadInt();
            beginTime = savefile.ReadInt();
            endTime = savefile.ReadInt();

            savefile.Read(beginPosition);
            savefile.Read(endPosition);

            savefile.Read(beginColor);
            savefile.Read(endColor);
        }

        public void Init(idGameSSDWindow _game, SSDEntity _ent, int _points, int _length, int _distance, final idVec4 color) {

            EntityInit();

            SetGame(_game);

            length = _length;
            distance = _distance;
            beginTime = game.ssdTime;
            endTime = beginTime + length;

            textScale = 0.4f;
            text = new idStr(va("%d", _points));

            float width = 0;
            for (int i = 0; i < text.Length(); i++) {
                width += game.GetDC().CharWidth(text.oGet(i), textScale);
            }

            size.Set(0, 0);

            //Set the start position at the top of the passed in entity
            position = WorldToScreen(_ent.position);
            position = ScreenToWorld(position);

            position.z = 0;
            position.x -= (width / 2.0f);

            beginPosition = position;

            endPosition = beginPosition;
            endPosition.y += _distance;

            //beginColor.Set(0,1,0,1);
            endColor.Set(1, 1, 1, 0);

            beginColor = color;
            beginColor.w = 1;

            noPlayerDamage = true;
            noHit = true;
        }

        @Override
        public void EntityUpdate() {

            float t = (float) (currentTime - beginTime) / (float) length;

            //Move up from the start position
            position.Lerp(beginPosition, endPosition, t);

            //Interpolate the color
            foreColor.Lerp(beginColor, endColor, t);

            if (currentTime > endTime) {
                destroyed = true;
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
                SSDPoints ent = GetSpecificPoints(id);
                ent.ReadFromSaveGame(savefile, _game);
            }
        }
//
        protected static final SSDPoints[] pointsPool = new SSDPoints[MAX_POINTS];
    };
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
            type = SSD_ENTITY_PROJECTILE;
        }
        // ~SSDProjectile();

        @Override
        public void WriteToSaveGame(idFile savefile) {
            super.WriteToSaveGame(savefile);

            savefile.Write(dir);
            savefile.Write(speed);
            savefile.WriteInt(beginTime);
            savefile.WriteInt(endTime);

            savefile.Write(endPosition);
        }

        @Override
        public void ReadFromSaveGame(idFile savefile, idGameSSDWindow _game) {
            super.ReadFromSaveGame(savefile, _game);

            savefile.Read(dir);
            savefile.Read(speed);
            beginTime = savefile.ReadInt();
            endTime = savefile.ReadInt();

            savefile.Read(endPosition);
        }

        public void Init(idGameSSDWindow _game, final idVec3 _beginPosition, final idVec3 _endPosition, float _speed, float _size) {

            EntityInit();

            SetGame(_game);

            SetMaterial(PROJECTILE_MATERIAL);
            size.Set(_size, _size);

            position = _beginPosition;
            endPosition = _endPosition;

            dir = _endPosition.oMinus(position);
            dir.Normalize();

            //speed.Zero();
            speed.x = speed.y = speed.z = _speed;

            noHit = true;
        }

        @Override
        public void EntityUpdate() {

            super.EntityUpdate();

            //Move forward based on speed (units per second)
            idVec3 moved = dir.oMultiply(((float) elapsed / 1000.0f) * speed.z);
            position.oPluSet(moved);

            if (position.z > endPosition.z) {
                //We have reached our position
                destroyed = true;
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
                SSDProjectile ent = GetSpecificProjectile(id);
                ent.ReadFromSaveGame(savefile, _game);
            }
        }
//
        protected static final SSDProjectile[] projectilePool = new SSDProjectile[MAX_PROJECTILES];
    };
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

            savefile.WriteInt(powerupState);
            savefile.WriteInt(powerupType);
        }

        @Override
        public void ReadFromSaveGame(idFile savefile, idGameSSDWindow _game) {
            super.ReadFromSaveGame(savefile, _game);

            powerupState = savefile.ReadInt();
            powerupType = savefile.ReadInt();
        }

        @Override
        public void OnHit(int key) {

            if (powerupState == POWERUP_STATE_CLOSED) {

                //Small explosion to indicate it is opened
                SSDExplosion explosion = SSDExplosion.GetNewExplosion(game, position, size.oMultiply(2.0f), 300, SSDExplosion.EXPLOSION_NORMAL, this, false, true);
                game.entities.Append(explosion);

                powerupState = POWERUP_STATE_OPEN;
                SetMaterial(powerupMaterials[powerupType][powerupState]);
            } else {
                //Destory the powerup with a big explosion
                SSDExplosion explosion = SSDExplosion.GetNewExplosion(game, position, size.oMultiply(2), 300, SSDExplosion.EXPLOSION_NORMAL, this);
                game.entities.Append(explosion);
                game.PlaySound("arcade_explode");

                noHit = true;
                noPlayerDamage = true;
            }
        }

        @Override
        public void OnStrikePlayer() {

            if (powerupState == POWERUP_STATE_OPEN) {
                //The powerup was open so activate it
                OnActivatePowerup();
            }

            //Just destroy the powerup
            destroyed = true;
        }

        public void OnOpenPowerup() {
        }

        public void OnActivatePowerup() {
            switch (powerupType) {
                case POWERUP_TYPE_HEALTH: {
                    game.AddHealth(10);
                    break;
                }
                case POWERUP_TYPE_SUPER_BLASTER: {
                    game.OnSuperBlaster();
                    break;
                }
                case POWERUP_TYPE_ASTEROID_NUKE: {
                    game.OnNuke();
                    break;
                }
                case POWERUP_TYPE_RESCUE_ALL: {
                    game.OnRescueAll();
                    break;
                }
                case POWERUP_TYPE_BONUS_POINTS: {
                    int points = (game.random.RandomInt(5) + 1) * 100;
                    game.AddScore(this, points);
                    break;
                }
                case POWERUP_TYPE_DAMAGE: {
                    game.AddDamage(10);
                    game.PlaySound("arcade_explode");
                    break;
                }

            }
        }

        public void Init(idGameSSDWindow _game, float _speed, float _rotation) {

            EntityInit();
            MoverInit(new idVec3(0, 0, -_speed), _rotation);

            SetGame(_game);
            SetSize(new idVec2(200, 200));
            SetRadius((float) Max(size.x, size.y), 0.3f);

            type = SSD_ENTITY_POWERUP;

            idVec3 startPosition = new idVec3();
            startPosition.x = game.random.RandomInt(V_WIDTH) - (V_WIDTH / 2.0f);
            startPosition.y = game.random.RandomInt(V_HEIGHT) - (V_HEIGHT / 2.0f);
            startPosition.z = ENTITY_START_DIST;

            position = startPosition;
            //SetPosition(startPosition);

            powerupState = POWERUP_STATE_CLOSED;
            powerupType = game.random.RandomInt(POWERUP_TYPE_MAX + 1);
            if (powerupType >= POWERUP_TYPE_MAX) {
                powerupType = 0;
            }

            /*OutputDebugString(va("Powerup: %d\n", powerupType));
             if(powerupType == 0) {
             int x = 0;
             }*/
            SetMaterial(powerupMaterials[powerupType][powerupState]);
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
                SSDPowerup ent = GetSpecificPowerup(id);
                ent.ReadFromSaveGame(savefile, _game);
            }
        }
//
        protected static final SSDPowerup[] powerupPool = new SSDPowerup[MAX_POWERUPS];
    };

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
    };

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
    };

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
    };

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
    };

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
    };

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
    };

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
    };

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

            savefile.WriteInt(ssdTime);

            beginLevel.WriteToSaveGame(savefile);
            resetGame.WriteToSaveGame(savefile);
            continueGame.WriteToSaveGame(savefile);
            refreshGuiData.WriteToSaveGame(savefile);

            crosshair.WriteToSaveGame(savefile);
            savefile.Write(screenBounds);

            savefile.WriteInt(levelCount);
            for (int i = 0; i < levelCount; i++) {
                savefile.Write(levelData.oGet(i));
                savefile.Write(asteroidData.oGet(i));
                savefile.Write(astronautData.oGet(i));
                savefile.Write(powerupData.oGet(i));
            }

            savefile.WriteInt(weaponCount);
            for (int i = 0; i < weaponCount; i++) {
                savefile.Write(weaponData.oGet(i));
            }

            savefile.WriteInt(superBlasterTimeout);
            savefile.Write(gameStats);

            //Write All Static Entities
            SSDAsteroid.WriteAsteroids(savefile);
            SSDAstronaut.WriteAstronauts(savefile);
            SSDExplosion.WriteExplosions(savefile);
            SSDPoints.WritePoints(savefile);
            SSDProjectile.WriteProjectiles(savefile);
            SSDPowerup.WritePowerups(savefile);

            int entCount = entities.Num();
            savefile.WriteInt(entCount);
            for (int i = 0; i < entCount; i++) {
                savefile.WriteInt(entities.oGet(i).type);
                savefile.WriteInt(entities.oGet(i).id);
            }
        }

        @Override
        public void ReadFromSaveGame(idFile savefile) {
            super.ReadFromSaveGame(savefile);

            ssdTime = savefile.ReadInt();

            beginLevel.ReadFromSaveGame(savefile);
            resetGame.ReadFromSaveGame(savefile);
            continueGame.ReadFromSaveGame(savefile);
            refreshGuiData.ReadFromSaveGame(savefile);

            crosshair.ReadFromSaveGame(savefile);
            savefile.Read(screenBounds);

            levelCount = savefile.ReadInt();
            for (int i = 0; i < levelCount; i++) {
                SSDLevelData_t newLevel = new SSDLevelData_t();
                savefile.Read(newLevel);
                levelData.Append(newLevel);

                SSDAsteroidData_t newAsteroid = new SSDAsteroidData_t();
                savefile.Read(newAsteroid);
                asteroidData.Append(newAsteroid);

                SSDAstronautData_t newAstronaut = new SSDAstronautData_t();
                savefile.Read(newAstronaut);
                astronautData.Append(newAstronaut);

                SSDPowerupData_t newPowerup = new SSDPowerupData_t();
                savefile.Read(newPowerup);
                powerupData.Append(newPowerup);
            }

            weaponCount = savefile.ReadInt();
            for (int i = 0; i < weaponCount; i++) {
                SSDWeaponData_t newWeapon = new SSDWeaponData_t();
                savefile.Read(newWeapon);
                weaponData.Append(newWeapon);
            }

            superBlasterTimeout = savefile.ReadInt();

            savefile.Read(gameStats);
            //Reset this because it is no longer valid
            gameStats.levelStats.targetEnt = null;

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

                SSDEntity ent = GetSpecificEntity(type, id);
                if (ent != null) {
                    entities.Append(ent);
                }
            }
        }

        @Override
        public String HandleEvent(final sysEvent_s event, boolean[] updateVisuals) {

            // need to call this to allow proper focus and capturing on embedded children
            final String ret = super.HandleEvent(event, updateVisuals);

            if (!gameStats.gameRunning) {
                return ret;
            }

            int key = event.evValue;

            if (event.evType == SE_KEY) {

                if (0 == event.evValue2) {
                    return ret;
                }

                if (key == K_MOUSE1 || key == K_MOUSE2) {
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
                retVar = beginLevel;
            }

            if (idStr.Icmp(_name, "resetGame") == 0) {
                retVar = resetGame;
            }

            if (idStr.Icmp(_name, "continueGame") == 0) {
                retVar = continueGame;
            }
            if (idStr.Icmp(_name, "refreshGuiData") == 0) {
                retVar = refreshGuiData;
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

            if (gameStats.gameRunning) {

                ZOrderEntities();

                //Draw from back to front
                for (int i = entities.Num() - 1; i >= 0; i--) {
                    entities.oGet(i).Draw(dc);
                }

                //The last thing to draw is the crosshair
                idVec2 cursor = new idVec2();
                //GetCursor(cursor);
                cursor.x = gui.CursorX();
                cursor.y = gui.CursorY();

                crosshair.Draw(dc, cursor);
            }
        }

        public void AddHealth(int health) {
            gameStats.health += health;
            gameStats.health = Min(100, gameStats.health);
        }

        public void AddScore(SSDEntity ent, int points) {

            SSDPoints pointsEnt;

            if (points > 0) {
                pointsEnt = SSDPoints.GetNewPoints(this, ent, points, 1000, 50, new idVec4(0, 1, 0, 1));
            } else {
                pointsEnt = SSDPoints.GetNewPoints(this, ent, points, 1000, 50, new idVec4(1, 0, 0, 1));
            }
            entities.Append(pointsEnt);

            gameStats.score += points;
            gui.SetStateString("player_score", va("%d", gameStats.score));
        }

        public void AddDamage(int damage) {
            gameStats.health -= damage;
            gui.SetStateString("player_health", va("%d", gameStats.health));

            gui.HandleNamedEvent("playerDamage");

            if (gameStats.health <= 0) {
                //The player is dead
                GameOver();
            }
        }

        public void OnNuke() {

            gui.HandleNamedEvent("nuke");

            //Destory All Asteroids
            for (int i = 0; i < entities.Num(); i++) {

                if (entities.oGet(i).type == SSD_ENTITY_ASTEROID) {

                    //The asteroid has been destroyed
                    SSDExplosion explosion = SSDExplosion.GetNewExplosion(this, entities.oGet(i).position, entities.oGet(i).size.oMultiply(2), 300, SSDExplosion.EXPLOSION_NORMAL, entities.oGet(i));
                    entities.Append(explosion);

                    AddScore(entities.oGet(i), asteroidData.oGet(gameStats.currentLevel).asteroidPoints);

                    //Don't let the player hit it anymore because 
                    entities.oGet(i).noHit = true;

                    gameStats.levelStats.destroyedAsteroids++;
                }
            }
            PlaySound("arcade_explode");

            //Check to see if a nuke ends the level
	/*if(gameStats.levelStats.destroyedAsteroids >= levelData[gameStats.currentLevel].needToWin) {
             LevelComplete();

             }*/
        }

        public void OnRescueAll() {

            gui.HandleNamedEvent("rescueAll");

            //Rescue All Astronauts
            for (int i = 0; i < entities.Num(); i++) {

                if (entities.oGet(i).type == SSD_ENTITY_ASTRONAUT) {

                    AstronautStruckPlayer((SSDAstronaut) entities.oGet(i));
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

            session.sw.PlayShaderDirectly(sound, currentSound);

            currentSound++;
            if (currentSound >= MAX_SOUND_CHANNEL) {
                currentSound = 0;
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
                beginLevel.oSet(src.ParseBool());
                return true;
            }
            if (idStr.Icmp(_name, "resetGame") == 0) {
                resetGame.oSet(src.ParseBool());
                return true;
            }
            if (idStr.Icmp(_name, "continueGame") == 0) {
                continueGame.oSet(src.ParseBool());
                return true;
            }
            if (idStr.Icmp(_name, "refreshGuiData") == 0) {
                refreshGuiData.oSet(src.ParseBool());
                return true;
            }

            if (idStr.Icmp(_name, "levelcount") == 0) {
                levelCount = src.ParseInt();
                for (int i = 0; i < levelCount; i++) {
                    SSDLevelData_t newLevel = new SSDLevelData_t();
//                    memset(newLevel, 0, sizeof(SSDLevelData_t));
                    levelData.Append(newLevel);

                    SSDAsteroidData_t newAsteroid = new SSDAsteroidData_t();
//                    memset(newAsteroid, 0, sizeof(SSDAsteroidData_t));
                    asteroidData.Append(newAsteroid);

                    SSDAstronautData_t newAstronaut = new SSDAstronautData_t();
//                    memset(newAstronaut, 0, sizeof(SSDAstronautData_t));
                    astronautData.Append(newAstronaut);

                    SSDPowerupData_t newPowerup = new SSDPowerupData_t();
//                    memset(newPowerup, 0, sizeof(SSDPowerupData_t));
                    powerupData.Append(newPowerup);

                }
                return true;
            }
            if (idStr.Icmp(_name, "weaponCount") == 0) {
                weaponCount = src.ParseInt();
                for (int i = 0; i < weaponCount; i++) {
                    SSDWeaponData_t newWeapon = new SSDWeaponData_t();
//                    memset(newWeapon, 0, sizeof(SSDWeaponData_t));
                    weaponData.Append(newWeapon);
                }
                return true;
            }

            if (idStr.FindText(_name, "leveldata", false) >= 0) {
                idStr tempName = new idStr(_name);
                int level = atoi(tempName.Right(2)) - 1;

                idStr levelData = new idStr();
                ParseString(src, levelData);
                ParseLevelData(level, levelData);
                return true;
            }

            if (idStr.FindText(_name, "asteroiddata", false) >= 0) {
                idStr tempName = new idStr(_name);
                int level = atoi(tempName.Right(2)) - 1;

                idStr asteroidData = new idStr();
                ParseString(src, asteroidData);
                ParseAsteroidData(level, asteroidData);
                return true;
            }

            if (idStr.FindText(_name, "weapondata", false) >= 0) {
                idStr tempName = new idStr(_name);
                int weapon = atoi(tempName.Right(2)) - 1;

                idStr weaponData = new idStr();
                ParseString(src, weaponData);
                ParseWeaponData(weapon, weaponData);
                return true;
            }

            if (idStr.FindText(_name, "astronautdata", false) >= 0) {
                idStr tempName = new idStr(_name);
                int level = atoi(tempName.Right(2)) - 1;

                idStr astronautData = new idStr();
                ParseString(src, astronautData);
                ParseAstronautData(level, astronautData);
                return true;
            }

            if (idStr.FindText(_name, "powerupdata", false) >= 0) {
                idStr tempName = new idStr(_name);
                int level = atoi(tempName.Right(2)) - 1;

                idStr powerupData = new idStr();
                ParseString(src, powerupData);
                ParsePowerupData(level, powerupData);
                return true;
            }

            return super.ParseInternalVar(_name, src);
        }

        private void ParseLevelData(int level, final idStr levelDataString) {

            idParser parser = new idParser();
            idToken token;
            parser.LoadMemory(levelDataString.toString(), levelDataString.Length(), "LevelData");

            levelData.oGet(level).spawnBuffer = parser.ParseFloat();
            levelData.oGet(level).needToWin = parser.ParseInt(); //Required Destroyed

        }

        private void ParseAsteroidData(int level, final idStr asteroidDataString) {

            idParser parser = new idParser();
            idToken token;
            parser.LoadMemory(asteroidDataString.toString(), asteroidDataString.Length(), "AsteroidData");

            asteroidData.oGet(level).speedMin = parser.ParseFloat(); //Speed Min 
            asteroidData.oGet(level).speedMax = parser.ParseFloat(); //Speed Max

            asteroidData.oGet(level).sizeMin = parser.ParseFloat(); //Size Min 
            asteroidData.oGet(level).sizeMax = parser.ParseFloat(); //Size Max

            asteroidData.oGet(level).rotateMin = parser.ParseFloat(); //Rotate Min (rotations per second) 
            asteroidData.oGet(level).rotateMax = parser.ParseFloat(); //Rotate Max (rotations per second)

            asteroidData.oGet(level).spawnMin = parser.ParseInt(); //Spawn Min
            asteroidData.oGet(level).spawnMax = parser.ParseInt(); //Spawn Max

            asteroidData.oGet(level).asteroidHealth = parser.ParseInt(); //Health of the asteroid
            asteroidData.oGet(level).asteroidDamage = parser.ParseInt(); //Asteroid Damage
            asteroidData.oGet(level).asteroidPoints = parser.ParseInt(); //Points awarded for destruction
        }

        private void ParseWeaponData(int weapon, final idStr weaponDataString) {

            idParser parser = new idParser();
            idToken token;
            parser.LoadMemory(weaponDataString.toString(), weaponDataString.Length(), "WeaponData");

            weaponData.oGet(weapon).speed = parser.ParseFloat();
            weaponData.oGet(weapon).damage = (int) parser.ParseFloat();
            weaponData.oGet(weapon).size = (int) parser.ParseFloat();
        }

        private void ParseAstronautData(int level, final idStr astronautDataString) {

            idParser parser = new idParser();
            idToken token;
            parser.LoadMemory(astronautDataString.toString(), astronautDataString.Length(), "AstronautData");

            astronautData.oGet(level).speedMin = parser.ParseFloat(); //Speed Min 
            astronautData.oGet(level).speedMax = parser.ParseFloat(); //Speed Max

            astronautData.oGet(level).rotateMin = parser.ParseFloat(); //Rotate Min (rotations per second) 
            astronautData.oGet(level).rotateMax = parser.ParseFloat(); //Rotate Max (rotations per second)

            astronautData.oGet(level).spawnMin = parser.ParseInt(); //Spawn Min
            astronautData.oGet(level).spawnMax = parser.ParseInt(); //Spawn Max

            astronautData.oGet(level).health = parser.ParseInt(); //Health of the asteroid
            astronautData.oGet(level).points = parser.ParseInt(); //Asteroid Damage
            astronautData.oGet(level).penalty = parser.ParseInt(); //Points awarded for destruction
        }

        private void ParsePowerupData(int level, final idStr powerupDataString) {

            idParser parser = new idParser();
            idToken token;
            parser.LoadMemory(powerupDataString.toString(), powerupDataString.Length(), "PowerupData");

            powerupData.oGet(level).speedMin = parser.ParseFloat(); //Speed Min 
            powerupData.oGet(level).speedMax = parser.ParseFloat(); //Speed Max

            powerupData.oGet(level).rotateMin = parser.ParseFloat(); //Rotate Min (rotations per second) 
            powerupData.oGet(level).rotateMax = parser.ParseFloat(); //Rotate Max (rotations per second)

            powerupData.oGet(level).spawnMin = parser.ParseInt(); //Spawn Min
            powerupData.oGet(level).spawnMax = parser.ParseInt(); //Spawn Max

        }

        private void CommonInit() {
            crosshair.InitCrosshairs();

            beginLevel.data = false;
            resetGame.data = false;
            continueGame.data = false;
            refreshGuiData.data = false;

            ssdTime = 0;
            levelCount = 0;
            weaponCount = 0;
            screenBounds = new idBounds(new idVec3(-320, -240, 0), new idVec3(320, 240, 0));

            superBlasterTimeout = 0;

            currentSound = 0;

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
            gameStats = new SSDGameStats_t();

            gameStats.health = 100;
        }

        private void ResetLevelStats() {

            ResetEntities();

            //Reset the level statistics structure
//            memset(gameStats.levelStats, 0, sizeof(gameStats.levelStats));
            gameStats.levelStats = new SSDLevelStats_t();
        }

        private void ResetEntities() {
            //Destroy all of the entities
            for (int i = 0; i < entities.Num(); i++) {
                entities.oGet(i).DestroyEntity();
            }
            entities.Clear();
        }

        //Game Running Methods
        private void StartGame() {

            gameStats.gameRunning = true;
        }

        private void StopGame() {

            gameStats.gameRunning = false;
        }

        private void GameOver() {

            StopGame();

            gui.HandleNamedEvent("gameOver");
        }

        //Starting the Game
        private void BeginLevel(int level) {

            ResetLevelStats();

            gameStats.currentLevel = level;

            StartGame();
        }

        /**
         * Continue game resets the players health
         */
        private void ContinueGame() {
            gameStats.health = 100;

            StartGame();
        }

        //Stopping the Game
        private void LevelComplete() {

            gameStats.prebonusscore = gameStats.score;

            // Add the bonuses
            int accuracy;
            if (0 == gameStats.levelStats.shotCount) {
                accuracy = 0;
            } else {
                accuracy = (int) (((float) gameStats.levelStats.hitCount / (float) gameStats.levelStats.shotCount) * 100.0f);
            }
            int accuracyPoints = Max(0, accuracy - 50) * 20;

            gui.SetStateString("player_accuracy_score", va("%d", accuracyPoints));

            gameStats.score += accuracyPoints;

            int saveAccuracy;
            int totalAst = gameStats.levelStats.savedAstronauts + gameStats.levelStats.killedAstronauts;
            if (0 == totalAst) {
                saveAccuracy = 0;
            } else {
                saveAccuracy = (int) (((float) gameStats.levelStats.savedAstronauts / (float) totalAst) * 100.0f);
            }
            accuracyPoints = Max(0, saveAccuracy - 50) * 20;

            gui.SetStateString("save_accuracy_score", va("%d", accuracyPoints));

            gameStats.score += accuracyPoints;

            StopSuperBlaster();

            gameStats.nextLevel++;

            if (gameStats.nextLevel >= levelCount) {
                //Have they beaten the game
                GameComplete();
            } else {

                //Make sure we don't go above the levelcount
                //min(gameStats.nextLevel, levelCount-1);
                StopGame();
                gui.HandleNamedEvent("levelComplete");
            }
        }

        private void GameComplete() {
            StopGame();
            gui.HandleNamedEvent("gameComplete");
        }

        private void UpdateGame() {

            //Check to see if and functions where called by the gui
            if (beginLevel.data == true) {
                beginLevel.data = false;
                BeginLevel(gameStats.nextLevel);
            }
            if (resetGame.data == true) {
                resetGame.data = false;
                ResetGameStats();
            }
            if (continueGame.data == true) {
                continueGame.data = false;
                ContinueGame();
            }
            if (refreshGuiData.data == true) {
                refreshGuiData.data = false;
                RefreshGuiData();
            }

            if (gameStats.gameRunning) {

                //We assume an upate every 16 milliseconds
                ssdTime += 16;

                if (superBlasterTimeout != 0 && ssdTime > superBlasterTimeout) {
                    StopSuperBlaster();
                }

                //Find if we are targeting and enemy
                idVec2 cursor = new idVec2();
                //GetCursor(cursor);
                cursor.x = gui.CursorX();
                cursor.y = gui.CursorY();
                gameStats.levelStats.targetEnt = EntityHitTest(cursor);

                //Update from back to front
                for (int i = entities.Num() - 1; i >= 0; i--) {
                    entities.oGet(i).Update();
                }

                CheckForHits();

                //Delete entities that need to be deleted
                for (int i = entities.Num() - 1; i >= 0; i--) {
                    if (entities.oGet(i).destroyed) {
                        SSDEntity ent = entities.oGet(i);
                        ent.DestroyEntity();
                        entities.RemoveIndex(i);
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
            for (int i = 0; i < entities.Num(); i++) {
                SSDEntity ent = entities.oGet(i);
                if (ent.position.z <= Z_NEAR) {

                    if (!ent.noPlayerDamage) {

                        //Is the object still in the screen
                        idVec3 entPos = ent.position;
                        entPos.z = 0;

                        idBounds entBounds = new idBounds(entPos);
                        entBounds.ExpandSelf(ent.hitRadius);

                        if (screenBounds.IntersectsBounds(entBounds)) {

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
            for (int i = entities.Num() - 1; i >= 0; i--) {
                boolean flipped = false;
                for (int j = 0; j < i; j++) {
                    if (entities.oGet(j).position.z > entities.oGet(j + 1).position.z) {
                        SSDEntity ent = entities.oGet(j);
                        entities.oSet(j, entities.oGet(j + 1));
                        entities.oSet(j + 1, ent);
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

            int currentTime = ssdTime;

            if (currentTime < gameStats.levelStats.nextAsteroidSpawnTime) {
                //Not time yet
                return;
            }

            //Lets spawn it
            idVec3 startPosition = new idVec3();

            float spawnBuffer = levelData.oGet(gameStats.currentLevel).spawnBuffer * 2.0f;
            startPosition.x = random.RandomInt(V_WIDTH + spawnBuffer) - ((V_WIDTH / 2.0f) + spawnBuffer);
            startPosition.y = random.RandomInt(V_HEIGHT + spawnBuffer) - ((V_HEIGHT / 2.0f) + spawnBuffer);
            startPosition.z = ENTITY_START_DIST;

            float speed = random.RandomInt(asteroidData.oGet(gameStats.currentLevel).speedMax - asteroidData.oGet(gameStats.currentLevel).speedMin) + asteroidData.oGet(gameStats.currentLevel).speedMin;
            float size = random.RandomInt(asteroidData.oGet(gameStats.currentLevel).sizeMax - asteroidData.oGet(gameStats.currentLevel).sizeMin) + asteroidData.oGet(gameStats.currentLevel).sizeMin;
            float rotate = (random.RandomFloat() * (asteroidData.oGet(gameStats.currentLevel).rotateMax - asteroidData.oGet(gameStats.currentLevel).rotateMin)) + asteroidData.oGet(gameStats.currentLevel).rotateMin;

            SSDAsteroid asteroid = SSDAsteroid.GetNewAsteroid(this, startPosition, new idVec2(size, size), speed, rotate, asteroidData.oGet(gameStats.currentLevel).asteroidHealth);
            entities.Append(asteroid);

            gameStats.levelStats.nextAsteroidSpawnTime = currentTime + random.RandomInt(asteroidData.oGet(gameStats.currentLevel).spawnMax - asteroidData.oGet(gameStats.currentLevel).spawnMin) + asteroidData.oGet(gameStats.currentLevel).spawnMin;
        }

        private void FireWeapon(int key) {

            idVec2 cursorWorld = GetCursorWorld();
            idVec2 cursor = new idVec2();
            //GetCursor(cursor);
            cursor.x = gui.CursorX();
            cursor.y = gui.CursorY();

            if (key == K_MOUSE1) {

                gameStats.levelStats.shotCount++;

                if (gameStats.levelStats.targetEnt != null) {
                    //Aim the projectile from the bottom of the screen directly at the ent
                    //SSDProjectile* newProj = new SSDProjectile(this, idVec3(320,0,0), gameStats.levelStats.targetEnt.position, weaponData[gameStats.currentWeapon].speed, weaponData[gameStats.currentWeapon].size);
                    SSDProjectile newProj = SSDProjectile.GetNewProjectile(this, new idVec3(0, -180, 0), gameStats.levelStats.targetEnt.position, weaponData.oGet(gameStats.currentWeapon).speed, weaponData.oGet(gameStats.currentWeapon).size);
                    entities.Append(newProj);
                    //newProj = SSDProjectile::GetNewProjectile(this, idVec3(-320,-0,0), gameStats.levelStats.targetEnt.position, weaponData[gameStats.currentWeapon].speed, weaponData[gameStats.currentWeapon].size);
                    //entities.Append(newProj);

                    //We hit something
                    gameStats.levelStats.hitCount++;

                    gameStats.levelStats.targetEnt.OnHit(key);

                    if (gameStats.levelStats.targetEnt.type == SSD_ENTITY_ASTEROID) {
                        HitAsteroid((SSDAsteroid) gameStats.levelStats.targetEnt, key);
                    } else if (gameStats.levelStats.targetEnt.type == SSD_ENTITY_ASTRONAUT) {
                        HitAstronaut((SSDAstronaut) gameStats.levelStats.targetEnt, key);
                    } else if (gameStats.levelStats.targetEnt.type == SSD_ENTITY_ASTRONAUT) {
                    }
                } else {
                    ////Aim the projectile at the cursor position all the way to the far clipping
                    //SSDProjectile* newProj = SSDProjectile::GetNewProjectile(this, idVec3(0,-180,0), idVec3(cursorWorld.x, cursorWorld.y, (Z_FAR-Z_NEAR)/2.0f), weaponData[gameStats.currentWeapon].speed, weaponData[gameStats.currentWeapon].size);

                    //Aim the projectile so it crosses the cursor 1/4 of screen
                    idVec3 vec = new idVec3(cursorWorld.x, cursorWorld.y, (Z_FAR - Z_NEAR) / 8.0f);
                    vec.oMulSet(8);
                    SSDProjectile newProj = SSDProjectile.GetNewProjectile(this, new idVec3(0, -180, 0), vec, weaponData.oGet(gameStats.currentWeapon).speed, weaponData.oGet(gameStats.currentWeapon).size);
                    entities.Append(newProj);

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

            for (int i = 0; i < entities.Num(); i++) {
                //Since we ZOrder the entities every frame we can stop at the first entity we hit.
                //ToDo: Make sure this assumption is true
                if (entities.oGet(i).HitTest(pt)) {
                    return entities.oGet(i);
                }
            }
            return null;
        }

        private void HitAsteroid(SSDAsteroid asteroid, int key) {

            asteroid.health -= weaponData.oGet(gameStats.currentWeapon).damage;

            if (asteroid.health <= 0) {

                //The asteroid has been destroyed
                SSDExplosion explosion = SSDExplosion.GetNewExplosion(this, asteroid.position, asteroid.size.oMultiply(2), 300, SSDExplosion.EXPLOSION_NORMAL, asteroid);
                entities.Append(explosion);
                PlaySound("arcade_explode");

                AddScore(asteroid, asteroidData.oGet(gameStats.currentLevel).asteroidPoints);

                //Don't let the player hit it anymore because 
                asteroid.noHit = true;

                gameStats.levelStats.destroyedAsteroids++;
                //if(gameStats.levelStats.destroyedAsteroids >= levelData[gameStats.currentLevel].needToWin) {
                //	LevelComplete();
                //}

            } else {
                //This was a damage hit so create a real small quick explosion
                SSDExplosion explosion = SSDExplosion.GetNewExplosion(this, asteroid.position, asteroid.size.oDivide(2.0f), 200, SSDExplosion.EXPLOSION_NORMAL, asteroid, false, false);
                entities.Append(explosion);
            }
        }

        private void AsteroidStruckPlayer(SSDAsteroid asteroid) {

            asteroid.noPlayerDamage = true;
            asteroid.noHit = true;

            AddDamage(asteroidData.oGet(gameStats.currentLevel).asteroidDamage);

            SSDExplosion explosion = SSDExplosion.GetNewExplosion(this, asteroid.position, asteroid.size.oMultiply(2), 300, SSDExplosion.EXPLOSION_NORMAL, asteroid);
            entities.Append(explosion);
            PlaySound("arcade_explode");
        }

        private void RefreshGuiData() {

            gui.SetStateString("nextLevel", va("%d", gameStats.nextLevel + 1));
            gui.SetStateString("currentLevel", va("%d", gameStats.currentLevel + 1));

            float accuracy;
            if (0 == gameStats.levelStats.shotCount) {
                accuracy = 0;
            } else {
                accuracy = ((float) gameStats.levelStats.hitCount / (float) gameStats.levelStats.shotCount) * 100.0f;
            }
            gui.SetStateString("player_accuracy", va("%d%%", (int) accuracy));

            float saveAccuracy;
            int totalAst = gameStats.levelStats.savedAstronauts + gameStats.levelStats.killedAstronauts;

            if (0 == totalAst) {
                saveAccuracy = 0;
            } else {
                saveAccuracy = ((float) gameStats.levelStats.savedAstronauts / (float) totalAst) * 100.0f;
            }
            gui.SetStateString("save_accuracy", va("%d%%", (int) saveAccuracy));

            if (gameStats.levelStats.targetEnt != null) {
                int dist = (int) (gameStats.levelStats.targetEnt.position.z / 100.0f);
                dist *= 100;
                gui.SetStateString("target_info", va("%d meters", dist));
            } else {
                gui.SetStateString("target_info", "No Target");
            }

            gui.SetStateString("player_health", va("%d", gameStats.health));
            gui.SetStateString("player_score", va("%d", gameStats.score));
            gui.SetStateString("player_prebonusscore", va("%d", gameStats.prebonusscore));
            gui.SetStateString("level_complete", va("%d/%d", gameStats.levelStats.savedAstronauts, levelData.oGet(gameStats.currentLevel).needToWin));

            if (superBlasterTimeout != 0) {
                float timeRemaining = (superBlasterTimeout - ssdTime) / 1000.0f;
                gui.SetStateString("super_blaster_time", va("%.2f", timeRemaining));
            }
        }

        private idVec2 GetCursorWorld() {

            idVec2 cursor = new idVec2();
            //GetCursor(cursor);
            cursor.x = gui.CursorX();
            cursor.y = gui.CursorY();
            cursor.x = cursor.x - 0.5f * V_WIDTH;
            cursor.y = -(cursor.y - 0.5f * V_HEIGHT);
            return cursor;
        }

        //Astronaut Methods
        private void SpawnAstronaut() {

            int currentTime = ssdTime;

            if (currentTime < gameStats.levelStats.nextAstronautSpawnTime) {
                //Not time yet
                return;
            }

            //Lets spawn it
            idVec3 startPosition = new idVec3();

            startPosition.x = random.RandomInt(V_WIDTH) - (V_WIDTH / 2.0f);
            startPosition.y = random.RandomInt(V_HEIGHT) - (V_HEIGHT / 2.0f);
            startPosition.z = ENTITY_START_DIST;

            float speed = random.RandomInt(astronautData.oGet(gameStats.currentLevel).speedMax - astronautData.oGet(gameStats.currentLevel).speedMin) + astronautData.oGet(gameStats.currentLevel).speedMin;
            float rotate = (random.RandomFloat() * (astronautData.oGet(gameStats.currentLevel).rotateMax - astronautData.oGet(gameStats.currentLevel).rotateMin)) + astronautData.oGet(gameStats.currentLevel).rotateMin;

            SSDAstronaut astronaut = SSDAstronaut.GetNewAstronaut(this, startPosition, speed, rotate, astronautData.oGet(gameStats.currentLevel).health);
            entities.Append(astronaut);

            gameStats.levelStats.nextAstronautSpawnTime = currentTime + random.RandomInt(astronautData.oGet(gameStats.currentLevel).spawnMax - astronautData.oGet(gameStats.currentLevel).spawnMin) + astronautData.oGet(gameStats.currentLevel).spawnMin;
        }

        private void HitAstronaut(SSDAstronaut astronaut, int key) {

            if (key == K_MOUSE1) {
                astronaut.health -= weaponData.oGet(gameStats.currentWeapon).damage;

                if (astronaut.health <= 0) {

                    gameStats.levelStats.killedAstronauts++;

                    //The astronaut has been destroyed
                    SSDExplosion explosion = SSDExplosion.GetNewExplosion(this, astronaut.position, astronaut.size.oMultiply(2), 300, SSDExplosion.EXPLOSION_NORMAL, astronaut);
                    entities.Append(explosion);
                    PlaySound("arcade_explode");

                    //Add the penalty for killing the astronaut
                    AddScore(astronaut, astronautData.oGet(gameStats.currentLevel).penalty);

                    //Don't let the player hit it anymore
                    astronaut.noHit = true;
                } else {
                    //This was a damage hit so create a real small quick explosion
                    SSDExplosion explosion = SSDExplosion.GetNewExplosion(this, astronaut.position, astronaut.size.oDivide(2.0f), 200, SSDExplosion.EXPLOSION_NORMAL, astronaut, false, false);
                    entities.Append(explosion);
                }
            }
        }

        private void AstronautStruckPlayer(SSDAstronaut astronaut) {

            gameStats.levelStats.savedAstronauts++;

            astronaut.noPlayerDamage = true;
            astronaut.noHit = true;

            //We are saving an astronaut
            SSDExplosion explosion = SSDExplosion.GetNewExplosion(this, astronaut.position, astronaut.size.oMultiply(2), 300, SSDExplosion.EXPLOSION_TELEPORT, astronaut);
            entities.Append(explosion);
            PlaySound("arcade_capture");

            //Give the player points for saving the astronaut
            AddScore(astronaut, astronautData.oGet(gameStats.currentLevel).points);

            if (gameStats.levelStats.savedAstronauts >= levelData.oGet(gameStats.currentLevel).needToWin) {
                LevelComplete();
            }

        }
        //Powerup Methods

        private void SpawnPowerup() {

            int currentTime = ssdTime;

            if (currentTime < gameStats.levelStats.nextPowerupSpawnTime) {
                //Not time yet
                return;
            }

            float speed = random.RandomInt(powerupData.oGet(gameStats.currentLevel).speedMax - powerupData.oGet(gameStats.currentLevel).speedMin) + powerupData.oGet(gameStats.currentLevel).speedMin;
            float rotate = (random.RandomFloat() * (powerupData.oGet(gameStats.currentLevel).rotateMax - powerupData.oGet(gameStats.currentLevel).rotateMin)) + powerupData.oGet(gameStats.currentLevel).rotateMin;

            SSDPowerup powerup = SSDPowerup.GetNewPowerup(this, speed, rotate);
            entities.Append(powerup);

            gameStats.levelStats.nextPowerupSpawnTime = currentTime + random.RandomInt(powerupData.oGet(gameStats.currentLevel).spawnMax - powerupData.oGet(gameStats.currentLevel).spawnMin) + powerupData.oGet(gameStats.currentLevel).spawnMin;
        }

        private void StartSuperBlaster() {

            gui.HandleNamedEvent("startSuperBlaster");
            gameStats.currentWeapon = 1;
            superBlasterTimeout = ssdTime + 10000;
        }

        private void StopSuperBlaster() {
            gui.HandleNamedEvent("stopSuperBlaster");
            gameStats.currentWeapon = 0;
            superBlasterTimeout = 0;
        }
        // void FreeSoundEmitter(bool immediate);
    };
}
