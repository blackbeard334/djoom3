package neo.framework;

import static neo.Renderer.ModelManager.renderModelManager;
import static neo.framework.Common.common;
import static neo.framework.DeclFX.fx_enum.FX_ATTACHENTITY;
import static neo.framework.DeclFX.fx_enum.FX_ATTACHLIGHT;
import static neo.framework.DeclFX.fx_enum.FX_DECAL;
import static neo.framework.DeclFX.fx_enum.FX_LAUNCH;
import static neo.framework.DeclFX.fx_enum.FX_LIGHT;
import static neo.framework.DeclFX.fx_enum.FX_MODEL;
import static neo.framework.DeclFX.fx_enum.FX_PARTICLE;
import static neo.framework.DeclFX.fx_enum.FX_SHAKE;
import static neo.framework.DeclFX.fx_enum.FX_SHOCKWAVE;
import static neo.framework.DeclFX.fx_enum.FX_SOUND;
import static neo.framework.DeclManager.DECL_LEXER_FLAGS;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_ENTITYDEF;

import neo.framework.DeclManager.idDecl;
import neo.idlib.Lib;
import neo.idlib.Lib.idException;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Vector;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Matrix.idMat3;

/**
 *
 */
public class DeclFX {
    /*
     ===============================================================================

     idDeclFX

     ===============================================================================
     */

    public enum fx_enum {

        FX_LIGHT,
        FX_PARTICLE,
        FX_DECAL,
        FX_MODEL,
        FX_SOUND,
        FX_SHAKE,
        FX_ATTACHLIGHT,
        FX_ATTACHENTITY,
        FX_LAUNCH,
        FX_SHOCKWAVE
    };

    //
    // single fx structure
    //
    public static class idFXSingleAction {

        public fx_enum type;
        public int sibling;
        //
        public idStr data;
        public idStr name;
        public idStr fire;
        //
        public float delay;
        public float duration;
        public float restart;
        public float size;
        public float fadeInTime;
        public float fadeOutTime;
        public float shakeTime;
        public float shakeAmplitude;
        public float shakeDistance;
        public float shakeImpulse;
        public float lightRadius;
        public float rotate;
        public float random1;
        public float random2;
        //
        public idVec3 lightColor;
        public idVec3 offset;
        public idMat3 axis;
        //
        public boolean soundStarted;
        public boolean shakeStarted;
        public boolean shakeFalloff;
        public boolean shakeIgnoreMaster;
        public boolean bindParticles;
        public boolean explicitAxis;
        public boolean noshadows;
        public boolean particleTrackVelocity;
        public boolean trackOrigin;
    };

    //
    // grouped fx structures
    //
    public static class idDeclFX extends idDecl {

        @Override
        public long Size() {
//            return sizeof( idDeclFX );
            return super.Size();
        }

        @Override
        public String DefaultDefinition() {
            {
                return "{\n"
                        + "\t" + "{\n"
                        + "\t\t" + "duration\t5\n"
                        + "\t\t" + "model\t\t_default\n"
                        + "\t" + "}\n"
                        + "}";
            }
        }

        @Override
        public boolean Parse(String text, int textLength) throws Lib.idException {
            idLexer src = new idLexer();

            src.LoadMemory(text, textLength, GetFileName(), GetLineNum());
            src.SetFlags(DECL_LEXER_FLAGS);
            src.SkipUntilString("{");

            // scan through, identifying each individual parameter
            while (true) {
                idToken token = new idToken();

                if (!src.ReadToken(token)) {
                    break;
                }

                if (token.equals("}")) {
                    break;
                }

                if (0 == token.Icmp("bindto")) {
                    src.ReadToken(token);
                    joint = token;
                    continue;
                }

                if (0 == token.Icmp("{")) {
                    idFXSingleAction action = new idFXSingleAction();
                    ParseSingleFXAction(src, action);
                    events.Append(action);
                    continue;
                }
            }

            if (src.HadError()) {
                src.Warning("FX decl '%s' had a parse error", GetName());
                return false;
            }
            return true;
        }

        @Override
        public void FreeData() {
            events.Clear();
        }

        @Override
        public void Print() throws Lib.idException {
            final idDeclFX list = this;
//            final fx_enum[] values = fx_enum.values();

            common.Printf("%d events\n", list.events.Num());
            for (int i = 0; i < list.events.Num(); i++) {
                switch (list.events.oGet(i).type) {
                    case FX_LIGHT:
                        common.Printf("FX_LIGHT %s\n", list.events.oGet(i).data.toString());
                        break;
                    case FX_PARTICLE:
                        common.Printf("FX_PARTICLE %s\n", list.events.oGet(i).data.toString());
                        break;
                    case FX_MODEL:
                        common.Printf("FX_MODEL %s\n", list.events.oGet(i).data.toString());
                        break;
                    case FX_SOUND:
                        common.Printf("FX_SOUND %s\n", list.events.oGet(i).data.toString());
                        break;
                    case FX_DECAL:
                        common.Printf("FX_DECAL %s\n", list.events.oGet(i).data.toString());
                        break;
                    case FX_SHAKE:
                        common.Printf("FX_SHAKE %s\n", list.events.oGet(i).data.toString());
                        break;
                    case FX_ATTACHLIGHT:
                        common.Printf("FX_ATTACHLIGHT %s\n", list.events.oGet(i).data.toString());
                        break;
                    case FX_ATTACHENTITY:
                        common.Printf("FX_ATTACHENTITY %s\n", list.events.oGet(i).data.toString());
                        break;
                    case FX_LAUNCH:
                        common.Printf("FX_LAUNCH %s\n", list.events.oGet(i).data.toString());
                        break;
                    case FX_SHOCKWAVE:
                        common.Printf("FX_SHOCKWAVE %s\n", list.events.oGet(i).data.toString());
                        break;
                }
            }
        }

        @Override
        public void List() throws idException {
            common.Printf("%s, %d stages\n", GetName(), events.Num());
        }
//
//
        public idList<idFXSingleAction> events = new idList<>();
        public idStr joint = new idStr();
//

        private void ParseSingleFXAction(idLexer src, idFXSingleAction FXAction) throws Lib.idException {
            idToken token = new idToken();

            FXAction.type = null;
            FXAction.sibling = -1;

            FXAction.data = new idStr("<none>");
            FXAction.name = new idStr("<none>");
            FXAction.fire = new idStr("<none>");

            FXAction.delay = 0.0f;
            FXAction.duration = 0.0f;
            FXAction.restart = 0.0f;
            FXAction.size = 0.0f;
            FXAction.fadeInTime = 0.0f;
            FXAction.fadeOutTime = 0.0f;
            FXAction.shakeTime = 0.0f;
            FXAction.shakeAmplitude = 0.0f;
            FXAction.shakeDistance = 0.0f;
            FXAction.shakeFalloff = false;
            FXAction.shakeImpulse = 0.0f;
            FXAction.shakeIgnoreMaster = false;
            FXAction.lightRadius = 0.0f;
            FXAction.rotate = 0.0f;
            FXAction.random1 = 0.0f;
            FXAction.random2 = 0.0f;

            FXAction.lightColor = Vector.getVec3_origin();
            FXAction.offset = Vector.getVec3_origin();
            FXAction.axis = idMat3.getMat3_identity();

            FXAction.bindParticles = false;
            FXAction.explicitAxis = false;
            FXAction.noshadows = false;
            FXAction.particleTrackVelocity = false;
            FXAction.trackOrigin = false;
            FXAction.soundStarted = false;

            while (true) {
                if (!src.ReadToken(token)) {
                    break;
                }

                if (0 == token.Icmp("}")) {
                    break;
                }

                if (0 == token.Icmp("shake")) {
                    FXAction.type = FX_SHAKE;
                    FXAction.shakeTime = src.ParseFloat();
                    src.ExpectTokenString(",");
                    FXAction.shakeAmplitude = src.ParseFloat();
                    src.ExpectTokenString(",");
                    FXAction.shakeDistance = src.ParseFloat();
                    src.ExpectTokenString(",");
                    FXAction.shakeFalloff = src.ParseBool();
                    src.ExpectTokenString(",");
                    FXAction.shakeImpulse = src.ParseFloat();
                    continue;
                }

                if (0 == token.Icmp("noshadows")) {
                    FXAction.noshadows = true;
                    continue;
                }

                if (0 == token.Icmp("name")) {
                    src.ReadToken(token);
                    FXAction.name.oSet(token);
                    continue;
                }

                if (0 == token.Icmp("fire")) {
                    src.ReadToken(token);
                    FXAction.fire.oSet(token);
                    continue;
                }

                if (0 == token.Icmp("random")) {
                    FXAction.random1 = src.ParseFloat();
                    src.ExpectTokenString(",");
                    FXAction.random2 = src.ParseFloat();
                    FXAction.delay = 0.0f;		// check random
                    continue;
                }

                if (0 == token.Icmp("delay")) {
                    FXAction.delay = src.ParseFloat();
                    continue;
                }

                if (0 == token.Icmp("rotate")) {
                    FXAction.rotate = src.ParseFloat();
                    continue;
                }

                if (0 == token.Icmp("duration")) {
                    FXAction.duration = src.ParseFloat();
                    continue;
                }

                if (0 == token.Icmp("trackorigin")) {
                    FXAction.trackOrigin = src.ParseBool();
                    continue;
                }

                if (0 == token.Icmp("restart")) {
                    FXAction.restart = src.ParseFloat();
                    continue;
                }

                if (0 == token.Icmp("fadeIn")) {
                    FXAction.fadeInTime = src.ParseFloat();
                    continue;
                }

                if (0 == token.Icmp("fadeOut")) {
                    FXAction.fadeOutTime = src.ParseFloat();
                    continue;
                }

                if (0 == token.Icmp("size")) {
                    FXAction.size = src.ParseFloat();
                    continue;
                }

                if (0 == token.Icmp("offset")) {
                    FXAction.offset.x = src.ParseFloat();
                    src.ExpectTokenString(",");
                    FXAction.offset.y = src.ParseFloat();
                    src.ExpectTokenString(",");
                    FXAction.offset.z = src.ParseFloat();
                    continue;
                }

                if (0 == token.Icmp("axis")) {
                    idVec3 v = new idVec3();
                    v.x = src.ParseFloat();
                    src.ExpectTokenString(",");
                    v.y = src.ParseFloat();
                    src.ExpectTokenString(",");
                    v.z = src.ParseFloat();
                    v.Normalize();
                    FXAction.axis.oSet(v.ToMat3());
                    FXAction.explicitAxis = true;
                    continue;
                }

                if (0 == token.Icmp("angle")) {
                    idAngles a = new idAngles();
                    a.oSet(0, src.ParseFloat());
                    src.ExpectTokenString(",");
                    a.oSet(1, src.ParseFloat());
                    src.ExpectTokenString(",");
                    a.oSet(2, src.ParseFloat());
                    FXAction.axis.oSet(a.ToMat3());
                    FXAction.explicitAxis = true;
                    continue;
                }

                if (0 == token.Icmp("uselight")) {
                    src.ReadToken(token);
                    FXAction.data.oSet(token);
                    for (int i = 0; i < events.Num(); i++) {
                        if (events.oGet(i).name.Icmp(FXAction.data.toString()) == 0) {
                            FXAction.sibling = i;
                            FXAction.lightColor.oSet(events.oGet(i).lightColor);
                            FXAction.lightRadius = events.oGet(i).lightRadius;
                        }
                    }
                    FXAction.type = FX_LIGHT;

                    // precache the light material
                    declManager.FindMaterial(FXAction.data);
                    continue;
                }

                if (0 == token.Icmp("attachlight")) {
                    src.ReadToken(token);
                    FXAction.data.oSet(token);
                    FXAction.type = FX_ATTACHLIGHT;

                    // precache it
                    declManager.FindMaterial(FXAction.data);
                    continue;
                }

                if (0 == token.Icmp("attachentity")) {
                    src.ReadToken(token);
                    FXAction.data.oSet(token);
                    FXAction.type = FX_ATTACHENTITY;

                    // precache the model
                    renderModelManager.FindModel(FXAction.data);
                    continue;
                }

                if (0 == token.Icmp("launch")) {
                    src.ReadToken(token);
                    FXAction.data.oSet(token);
                    FXAction.type = FX_LAUNCH;

                    // precache the entity def
                    declManager.FindType(DECL_ENTITYDEF, FXAction.data);
                    continue;
                }

                if (0 == token.Icmp("useModel")) {
                    src.ReadToken(token);
                    FXAction.data.oSet(token);
                    for (int i = 0; i < events.Num(); i++) {
                        if (events.oGet(i).name.Icmp(FXAction.data) == 0) {
                            FXAction.sibling = i;
                        }
                    }
                    FXAction.type = FX_MODEL;

                    // precache the model
                    renderModelManager.FindModel(FXAction.data);
                    continue;
                }

                if (0 == token.Icmp("light")) {
                    src.ReadToken(token);
                    FXAction.data.oSet(token);
                    src.ExpectTokenString(",");
                    FXAction.lightColor.oSet(0, src.ParseFloat());
                    src.ExpectTokenString(",");
                    FXAction.lightColor.oSet(1, src.ParseFloat());
                    src.ExpectTokenString(",");
                    FXAction.lightColor.oSet(2, src.ParseFloat());
                    src.ExpectTokenString(",");
                    FXAction.lightRadius = src.ParseFloat();
                    FXAction.type = FX_LIGHT;

                    // precache the light material
                    declManager.FindMaterial(FXAction.data);
                    continue;
                }

                if (0 == token.Icmp("model")) {
                    src.ReadToken(token);
                    FXAction.data.oSet(token);
                    FXAction.type = FX_MODEL;

                    // precache it
                    renderModelManager.FindModel(FXAction.data.toString());
                    continue;
                }

                if (0 == token.Icmp("particle")) {	// FIXME: now the same as model
                    src.ReadToken(token);
                    FXAction.data.oSet(token);
                    FXAction.type = FX_PARTICLE;

                    // precache it
                    renderModelManager.FindModel(FXAction.data.toString());
                    continue;
                }

                if (0 == token.Icmp("decal")) {
                    src.ReadToken(token);
                    FXAction.data.oSet(token);
                    FXAction.type = FX_DECAL;

                    // precache it
                    declManager.FindMaterial(FXAction.data);
                    continue;
                }

                if (0 == token.Icmp("particleTrackVelocity")) {
                    FXAction.particleTrackVelocity = true;
                    continue;
                }

                if (0 == token.Icmp("sound")) {
                    src.ReadToken(token);
                    FXAction.data.oSet(token);
                    FXAction.type = FX_SOUND;

                    // precache it
                    declManager.FindSound(FXAction.data);
                    continue;
                }

                if (0 == token.Icmp("ignoreMaster")) {
                    FXAction.shakeIgnoreMaster = true;
                    continue;
                }

                if (0 == token.Icmp("shockwave")) {
                    src.ReadToken(token);
                    FXAction.data.oSet(token);
                    FXAction.type = FX_SHOCKWAVE;

                    // precache the entity def
                    declManager.FindType(DECL_ENTITYDEF, FXAction.data);
                    continue;
                }

                src.Warning("FX File: bad token");
                continue;
            }
        }

        public void oSet(idDeclFX idDeclFX) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    };
}
