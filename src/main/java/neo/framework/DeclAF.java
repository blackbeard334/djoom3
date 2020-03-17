package neo.framework;

import static neo.Renderer.Material.CONTENTS_BODY;
import static neo.Renderer.Material.CONTENTS_CORPSE;
import static neo.Renderer.Material.CONTENTS_MONSTERCLIP;
import static neo.Renderer.Material.CONTENTS_MOVEABLECLIP;
import static neo.Renderer.Material.CONTENTS_PLAYERCLIP;
import static neo.Renderer.Material.CONTENTS_SOLID;
import static neo.framework.Common.common;
import static neo.framework.DeclAF.declAFConstraintType_t.DECLAF_CONSTRAINT_BALLANDSOCKETJOINT;
import static neo.framework.DeclAF.declAFConstraintType_t.DECLAF_CONSTRAINT_FIXED;
import static neo.framework.DeclAF.declAFConstraintType_t.DECLAF_CONSTRAINT_HINGE;
import static neo.framework.DeclAF.declAFConstraintType_t.DECLAF_CONSTRAINT_SLIDER;
import static neo.framework.DeclAF.declAFConstraintType_t.DECLAF_CONSTRAINT_SPRING;
import static neo.framework.DeclAF.declAFConstraintType_t.DECLAF_CONSTRAINT_UNIVERSALJOINT;
import static neo.framework.DeclAF.declAFJointMod_t.DECLAF_JOINTMOD_AXIS;
import static neo.framework.DeclAF.declAFJointMod_t.DECLAF_JOINTMOD_BOTH;
import static neo.framework.DeclAF.declAFJointMod_t.DECLAF_JOINTMOD_ORIGIN;
import static neo.framework.DeclAF.idAFVector.type.VEC_BONECENTER;
import static neo.framework.DeclAF.idAFVector.type.VEC_BONEDIR;
import static neo.framework.DeclAF.idAFVector.type.VEC_COORDS;
import static neo.framework.DeclAF.idAFVector.type.VEC_JOINT;
import static neo.framework.DeclManager.DECL_LEXER_FLAGS;
import static neo.idlib.Text.Token.TT_NAME;
import static neo.idlib.Text.Token.TT_STRING;
import static neo.idlib.geometry.TraceModel.traceModel_t.TRM_BONE;
import static neo.idlib.geometry.TraceModel.traceModel_t.TRM_BOX;
import static neo.idlib.geometry.TraceModel.traceModel_t.TRM_CONE;
import static neo.idlib.geometry.TraceModel.traceModel_t.TRM_CYLINDER;
import static neo.idlib.geometry.TraceModel.traceModel_t.TRM_DODECAHEDRON;
import static neo.idlib.geometry.TraceModel.traceModel_t.TRM_INVALID;
import static neo.idlib.geometry.TraceModel.traceModel_t.TRM_OCTAHEDRON;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;

import neo.framework.DeclManager.idDecl;
import neo.framework.File_h.idFile;
import neo.framework.File_h.idFile_Memory;
import neo.idlib.Lib.idException;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.List.idList;
import neo.idlib.geometry.JointTransform.idJointMat;
import neo.idlib.geometry.TraceModel.traceModel_t;
import neo.idlib.math.Angles;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Vector;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Matrix.idMat3;

/**
 *
 */
public class DeclAF {

    /*
     ===============================================================================

     Articulated Figure

     ===============================================================================
     */
    public enum declAFConstraintType_t {

        DECLAF_CONSTRAINT_INVALID,
        DECLAF_CONSTRAINT_FIXED,
        DECLAF_CONSTRAINT_BALLANDSOCKETJOINT,
        DECLAF_CONSTRAINT_UNIVERSALJOINT,
        DECLAF_CONSTRAINT_HINGE,
        DECLAF_CONSTRAINT_SLIDER,
        DECLAF_CONSTRAINT_SPRING
    };

    public enum declAFJointMod_t {

        DECLAF_JOINTMOD_AXIS,
        DECLAF_JOINTMOD_ORIGIN,
        DECLAF_JOINTMOD_BOTH
    };

    public static abstract class getJointTransform_t {

        public abstract boolean run(Object model, final idJointMat[] frame, final String jointName, idVec3 origin, idMat3 axis);

        public abstract boolean run(Object model, final idJointMat[] frame, final idStr jointName, idVec3 origin, idMat3 axis);//TODO:phase out overload
    };

    public static class idAFVector {

        enum type {

            VEC_COORDS,
            VEC_JOINT,
            VEC_BONECENTER,
            VEC_BONEDIR
        };
        public  type    type;
        public  idStr   joint1;
        public  idStr   joint2;
        private idVec3  vec;
        private boolean negate;
        //
        //
        private static int DBG_counter = 0;
        private final  int DBG_count = DBG_counter++;

        public idAFVector() {
            type = VEC_COORDS;
            joint1 = new idStr();
            joint2 = new idStr();
            vec = new idVec3();
            negate = false;
        }

        public boolean Parse(idLexer src) throws idException {
            idToken token = new idToken();

            if (!src.ReadToken(token)) {
                return false;
            }

            if (token.equals("-")) {
                negate = true;
                if (!src.ReadToken(token)) {
                    return false;
                }
            } else {
                negate = false;
            }

            if (token.equals("(")) {
                type = VEC_COORDS;
                vec.x = src.ParseFloat();
                src.ExpectTokenString(",");
                vec.y = src.ParseFloat();
                src.ExpectTokenString(",");
                vec.z = src.ParseFloat();
                src.ExpectTokenString(")");
            } else if (token.equals("joint")) {
                type = VEC_JOINT;
                src.ExpectTokenString("(");
                src.ReadToken(token);
                joint1.oSet(token);
                src.ExpectTokenString(")");
            } else if (token.equals("bonecenter")) {
                type = VEC_BONECENTER;
                src.ExpectTokenString("(");
                src.ReadToken(token);
                joint1.oSet(token);
                src.ExpectTokenString(",");
                src.ReadToken(token);
                joint2.oSet(token);
                src.ExpectTokenString(")");
            } else if (token.equals("bonedir")) {
                type = VEC_BONEDIR;
                src.ExpectTokenString("(");
                src.ReadToken(token);
                joint1.oSet(token);
                src.ExpectTokenString(",");
                src.ReadToken(token);
                joint2.oSet(token);
                src.ExpectTokenString(")");
            } else {
                src.Error("unknown token %s in vector", token.getData());
                return false;
            }

            return true;
        }

        public boolean Finish(final String fileName, final getJointTransform_t GetJointTransform, final idJointMat[] frame, Object model) throws idException {
            idMat3 axis = new idMat3();
            idVec3 start = new idVec3(), end = new idVec3();

            switch (type) {
                case VEC_COORDS: {
                    break;
                }
                case VEC_JOINT: {
                    if (!GetJointTransform.run(model, frame, joint1, vec, axis)) {
                        common.Warning("invalid joint %s in joint() in '%s'", joint1.getData(), fileName);
                        vec.Zero();
                    }
                    break;
                }
                case VEC_BONECENTER: {
                    if (!GetJointTransform.run(model, frame, joint1, start, axis)) {
                        common.Warning("invalid joint %s in bonecenter() in '%s'", joint1.getData(), fileName);
                        start.Zero();
                    }
                    if (!GetJointTransform.run(model, frame, joint2, end, axis)) {
                        common.Warning("invalid joint %s in bonecenter() in '%s'", joint2.getData(), fileName);
                        end.Zero();
                    }
                    vec.oSet((start.oPlus(end)).oMultiply(0.5f));
                    break;
                }
                case VEC_BONEDIR: {
                    if (!GetJointTransform.run(model, frame, joint1, start, axis)) {
                        common.Warning("invalid joint %s in bonedir() in '%s'", joint1.getData(), fileName);
                        start.Zero();
                    }
                    if (!GetJointTransform.run(model, frame, joint2, end, axis)) {
                        common.Warning("invalid joint %s in bonedir() in '%s'", joint2.getData(), fileName);
                        end.Zero();
                    }
                    vec.oSet((end.oMinus(start)));
                    break;
                }
                default: {
                    vec.Zero();
                    break;
                }
            }

            if (negate) {
                vec.oSet(vec.oNegative());
            }

            return true;
        }

        public boolean Write(idFile f) {

            if (negate) {
                f.WriteFloatString("-");
            }
            switch (type) {
                case VEC_COORDS: {
                    f.WriteFloatString("( %f, %f, %f )", vec.x, vec.y, vec.z);
                    break;
                }
                case VEC_JOINT: {
                    f.WriteFloatString("joint( \"%s\" )", joint1.getData());
                    break;
                }
                case VEC_BONECENTER: {
                    f.WriteFloatString("bonecenter( \"%s\", \"%s\" )", joint1.getData(), joint2.getData());
                    break;
                }
                case VEC_BONEDIR: {
                    f.WriteFloatString("bonedir( \"%s\", \"%s\" )", joint1.getData(), joint2.getData());
                    break;
                }
                default: {
                    break;
                }
            }
            return true;
        }

        public String ToString(idStr str, final int precision /*= 8*/) {

            switch (type) {
                case VEC_COORDS: {
                    String format;//[128];
                    format = String.format("( %%.%df, %%.%df, %%.%df )", precision, precision, precision);
                    str.oSet(String.format(format, vec.x, vec.y, vec.z));
                    break;
                }
                case VEC_JOINT: {
                    str.oSet(String.format("joint( \"%s\" )", joint1.getData()));
                    break;
                }
                case VEC_BONECENTER: {
                    str.oSet(String.format("bonecenter( \"%s\", \"%s\" )", joint1.getData(), joint2.getData()));
                    break;
                }
                case VEC_BONEDIR: {
                    str.oSet(String.format("bonedir( \"%s\", \"%s\" )", joint1.getData(), joint2.getData()));
                    break;
                }
                default: {
                    break;
                }
            }
            if (negate) {
                str.oSet("-" + str.getData());//TODO:don't set= idStr reference
            }
            return str.getData();
        }

        public idVec3 ToVec3() {
            return vec;
        }
//public	idVec3 &				ToVec3( void ) { return vec; }
    };

    public static class idDeclAF_Body {

        public idStr            name;
        public idStr            jointName;
        public declAFJointMod_t jointMod;
        public traceModel_t     modelType;
        public idAFVector       v1, v2;
        public int        numSides;
        public float      width;
        public float      density;
        public idAFVector origin;
        public idAngles   angles;
        public int[] contents = {0};
        public int[] clipMask = {0};
        public boolean    selfCollision;
        public idMat3     inertiaScale;
        public float      linearFriction;
        public float      angularFriction;
        public float      contactFriction;
        public idStr      containedJoints;
        public idAFVector frictionDirection;
        public idAFVector contactMotorDirection;
//

        public idDeclAF_Body() {
            this.name = new idStr();
            this.jointName = new idStr();
            this.containedJoints = new idStr();
        }

        public void SetDefault(final idDeclAF file) {
            name.oSet("noname");
            modelType = TRM_BOX;
            v1 = new idAFVector();
            v1.ToVec3().x = v1.ToVec3().y = v1.ToVec3().z = -10.0f;
            v2 = new idAFVector();
            v2.ToVec3().x = v2.ToVec3().y = v2.ToVec3().z = 10.0f;
            numSides = 3;
            origin = new idAFVector();
            angles = new idAngles();
            density = 0.2f;
            inertiaScale = getMat3_identity();
            linearFriction = file.defaultLinearFriction;
            angularFriction = file.defaultAngularFriction;
            contactFriction = file.defaultContactFriction;
            contents = file.contents;
            clipMask = file.clipMask;
            selfCollision = file.selfCollision;
            frictionDirection = new idAFVector();
            contactMotorDirection = new idAFVector();
            jointName.oSet("origin");
            jointMod = DECLAF_JOINTMOD_AXIS;
            containedJoints.oSet("origin");
        }
    };

    public static class idDeclAF_Constraint {

        public idStr                  name = new idStr();
        public idStr                  body1 = new idStr();
        public idStr                  body2 = new idStr();
        public declAFConstraintType_t type;
        public float                  friction;
        public float                  stretch;
        public float                  compress;
        public float                  damping;
        public float                  restLength;
        public float                  minLength;
        public float                  maxLength;
        public idAFVector             anchor;
        public idAFVector             anchor2;
        public              idAFVector[] shaft         = {new idAFVector(), new idAFVector()};
        public              idAFVector   axis          = new idAFVector();
        //
        public static final int          LIMIT_NONE    = -1;
        public static final int          LIMIT_CONE    = 0;
        public static final int          LIMIT_PYRAMID = 1;
        //			
        public int limit;
        public idAFVector limitAxis   = new idAFVector();
        public float[]    limitAngles = new float[3];
        //
        //

        public void SetDefault(final idDeclAF file) {
            name.oSet("noname");
            type = DECLAF_CONSTRAINT_UNIVERSALJOINT;
            if (file.bodies.Num() != 0) {
                body1.oSet(file.bodies.oGet(0).name);
            } else {
                body1.oSet("world");
            }
            body2.oSet("world");
            friction = file.defaultConstraintFriction;
            anchor = new idAFVector();
            anchor2 = new idAFVector();
            axis.ToVec3().Set(1.0f, 0.0f, 0.0f);
            shaft[0].ToVec3().Set(0.0f, 0.0f, -1.0f);
            shaft[1].ToVec3().Set(0.0f, 0.0f, 1.0f);
            limit = LIMIT_NONE;
            limitAngles[0] = limitAngles[1] = limitAngles[2] = 0.0f;
            limitAxis.ToVec3().Set(0.0f, 0.0f, -1.0f);
        }
    };

    public static class idDeclAF extends idDecl {

        public boolean modified;
        public idStr   model;
        public idStr   skin;
        public float   defaultLinearFriction;
        public float   defaultAngularFriction;
        public float   defaultContactFriction;
        public float   defaultConstraintFriction;
        public float   totalMass;
        public idVec2 suspendVelocity     = new idVec2();
        public idVec2 suspendAcceleration = new idVec2();
        public float noMoveTime;
        public float noMoveTranslation;
        public float noMoveRotation;
        public float minMoveTime;
        public float maxMoveTime;
        public int[] contents = new int[1];
        public int[] clipMask = new int[1];
        public boolean selfCollision;
        public final idList<idDeclAF_Body>       bodies      = new idList<>(idDeclAF_Body.class);
        public final idList<idDeclAF_Constraint> constraints = new idList<>(idDeclAF_Constraint.class);
        //
        //

        public idDeclAF() {
            FreeData();
        }
//public virtual					~idDeclAF( void );
// 

        @Override
        public long Size() {
//            return sizeof( idDeclAF );
            return super.Size();
        }

        @Override
        public String DefaultDefinition() {
            return "{\n"
                    + "\t" + "settings {\n"
                    + "\t\t" + "model \"\"\n"
                    + "\t\t" + "skin \"\"\n"
                    + "\t\t" + "friction 0.01, 0.01, 0.8, 0.5\n"
                    + "\t\t" + "suspendSpeed 20, 30, 40, 60\n"
                    + "\t\t" + "noMoveTime 1\n"
                    + "\t\t" + "noMoveTranslation 10\n"
                    + "\t\t" + "noMoveRotation 10\n"
                    + "\t\t" + "minMoveTime -1\n"
                    + "\t\t" + "maxMoveTime -1\n"
                    + "\t\t" + "totalMass -1\n"
                    + "\t\t" + "contents corpse\n"
                    + "\t\t" + "clipMask solid, corpse\n"
                    + "\t\t" + "selfCollision 1\n"
                    + "\t" + "}\n"
                    + "\t" + "body \"body\" {\n"
                    + "\t\t" + "joint \"origin\"\n"
                    + "\t\t" + "mod orientation\n"
                    + "\t\t" + "model box( ( -10, -10, -10 ), ( 10, 10, 10 ) )\n"
                    + "\t\t" + "origin ( 0, 0, 0 )\n"
                    + "\t\t" + "density 0.2\n"
                    + "\t\t" + "friction 0.01, 0.01, 0.8\n"
                    + "\t\t" + "contents corpse\n"
                    + "\t\t" + "clipMask solid, corpse\n"
                    + "\t\t" + "selfCollision 1\n"
                    + "\t\t" + "containedJoints \"*origin\"\n"
                    + "\t" + "}\n"
                    + "}\n";
        }

        @Override
        public boolean Parse(String text, int textLength) throws idException {
            int i, j;
            idLexer src = new idLexer();
            idToken token = new idToken();

            src.LoadMemory(text, textLength, GetFileName(), GetLineNum());
            src.SetFlags(DECL_LEXER_FLAGS);
            src.SkipUntilString("{");

            while (src.ReadToken(token)) {

                if (0 == token.Icmp("settings")) {
                    if (!ParseSettings(src)) {
                        return false;
                    }
                } else if (0 == token.Icmp("body")) {
                    if (!ParseBody(src)) {
                        return false;
                    }
                } else if (0 == token.Icmp("fixed")) {
                    if (!ParseFixed(src)) {
                        return false;
                    }
                } else if (0 == token.Icmp("ballAndSocketJoint")) {
                    if (!ParseBallAndSocketJoint(src)) {
                        return false;
                    }
                } else if (0 == token.Icmp("universalJoint")) {
                    if (!ParseUniversalJoint(src)) {
                        return false;
                    }
                } else if (0 == token.Icmp("hinge")) {
                    if (!ParseHinge(src)) {
                        return false;
                    }
                } else if (0 == token.Icmp("slider")) {
                    if (!ParseSlider(src)) {
                        return false;
                    }
                } else if (0 == token.Icmp("spring")) {
                    if (!ParseSpring(src)) {
                        return false;
                    }
                } else if (token.equals("}")) {
                    break;
                } else {
                    src.Error("unknown keyword %s", token);
                    return false;
                }
            }

            for (i = 0; i < bodies.Num(); i++) {
                // check for multiple bodies with the same name
                for (j = i + 1; j < bodies.Num(); j++) {
                    if (bodies.oGet(i).name == bodies.oGet(j).name) {
                        src.Error("two bodies with the same name \"%s\"", bodies.oGet(i).name);
                    }
                }
            }

            for (i = 0; i < constraints.Num(); i++) {
                // check for multiple constraints with the same name
                for (j = i + 1; j < constraints.Num(); j++) {
                    if (constraints.oGet(i).name == constraints.oGet(j).name) {
                        src.Error("two constraints with the same name \"%s\"", constraints.oGet(i).name);
                    }
                }
                // check if there are two valid bodies set
                if (constraints.oGet(i).body1.IsEmpty()) {
                    src.Error("no valid body1 specified for constraint '%s'", constraints.oGet(i).name);
                }
                if (constraints.oGet(i).body2.IsEmpty()) {
                    src.Error("no valid body2 specified for constraint '%s'", constraints.oGet(i).name);
                }
            }

            // make sure the body which modifies the origin comes first
            for (i = 0; i < bodies.Num(); i++) {
                if (bodies.oGet(i).jointName.equals("origin")) {
                    if (i != 0) {
                        idDeclAF_Body b = bodies.oGet(0);
                        bodies.oSet(0, bodies.oGet(i));
                        bodies.oSet(i, b);
                    }
                    break;
                }
            }

            return true;
        }
//public virtual void			FreeData( void );

        @Override
        public void FreeData() {
            modified = false;
            defaultLinearFriction = 0.01f;
            defaultAngularFriction = 0.01f;
            defaultContactFriction = 0.8f;
            defaultConstraintFriction = 0.5f;
            totalMass = -1;
            suspendVelocity.Set(20.0f, 30.0f);
            suspendAcceleration.Set(40.0f, 60.0f);
            noMoveTime = 1.0f;
            noMoveTranslation = 10.0f;
            noMoveRotation = 10.0f;
            minMoveTime = -1.0f;
            maxMoveTime = -1.0f;
            selfCollision = true;
            contents[0] = CONTENTS_CORPSE;
            clipMask[0] = CONTENTS_SOLID | CONTENTS_CORPSE;
            bodies.DeleteContents(true);
            constraints.DeleteContents(true);
        }

        public /*virtual */ void Finish(final getJointTransform_t GetJointTransform, final idJointMat[] frame, Object model) throws idException {
            int i;

            final String name = GetName();
            for (i = 0; i < bodies.Num(); i++) {
                idDeclAF_Body body = bodies.oGet(i);
                body.v1.Finish(name, GetJointTransform, frame, model);
                body.v2.Finish(name, GetJointTransform, frame, model);
                body.origin.Finish(name, GetJointTransform, frame, model);
                body.frictionDirection.Finish(name, GetJointTransform, frame, model);
                body.contactMotorDirection.Finish(name, GetJointTransform, frame, model);
            }
            for (i = 0; i < constraints.Num(); i++) {
                idDeclAF_Constraint constraint = constraints.oGet(i);
                constraint.anchor.Finish(name, GetJointTransform, frame, model);
                constraint.anchor2.Finish(name, GetJointTransform, frame, model);
                constraint.shaft[0].Finish(name, GetJointTransform, frame, model);
                constraint.shaft[1].Finish(name, GetJointTransform, frame, model);
                constraint.axis.Finish(name, GetJointTransform, frame, model);
                constraint.limitAxis.Finish(name, GetJointTransform, frame, model);
            }
        }

        public boolean Save() throws idException {
            RebuildTextSource();
            ReplaceSourceFileText();
            modified = false;
            return true;
        }
// 

        public void NewBody(final String name) {
            idDeclAF_Body body;

            body = new idDeclAF_Body();
            body.SetDefault(this);
            body.name.oSet(name);
            bodies.Append(body);
        }

        /*
         ================
         idDeclAF::RenameBody

         rename the body with the given name and rename
         all constraint body references
         ================
         */
        public void RenameBody(final String oldName, final String newName) {
            int i;

            for (i = 0; i < bodies.Num(); i++) {
                if (bodies.oGet(i).name.Icmp(oldName) == 0) {
                    bodies.oGet(i).name.oSet(newName);
                    break;
                }
            }
            for (i = 0; i < constraints.Num(); i++) {
                if (constraints.oGet(i).body1.Icmp(oldName) == 0) {
                    constraints.oGet(i).body1.oSet(newName);
                } else if (constraints.oGet(i).body2.Icmp(oldName) == 0) {
                    constraints.oGet(i).body2.oSet(newName);
                }
            }
        }

        /*
         ================
         idDeclAF::DeleteBody

         delete the body with the given name and delete
         all constraints that reference the body
         ================
         */
        public void DeleteBody(final String name) {
            int i;

            for (i = 0; i < bodies.Num(); i++) {
                if (bodies.oGet(i).name.Icmp(name) == 0) {
//			delete bodies.oGet(i);
                    bodies.RemoveIndex(i);
                    break;
                }
            }
            for (i = 0; i < constraints.Num(); i++) {
                if (constraints.oGet(i).body1.Icmp(name) == 0
                        || constraints.oGet(i).body2.Icmp(name) == 0) {
//			delete constraints.oGet(i);
                    constraints.RemoveIndex(i);
                    i--;
                }
            }
        }
// 

        public void NewConstraint(final String name) {
            idDeclAF_Constraint constraint;

            constraint = new idDeclAF_Constraint();
            constraint.SetDefault(this);
            constraint.name.oSet(name);
            constraints.Append(constraint);
        }

        public void RenameConstraint(final String oldName, final String newName) {
            int i;

            for (i = 0; i < constraints.Num(); i++) {
                if (constraints.oGet(i).name.Icmp(oldName) == 0) {
                    constraints.oGet(i).name.oSet(newName);
                    return;
                }
            }
        }

        public void DeleteConstraint(final String name) {
            int i;

            for (i = 0; i < constraints.Num(); i++) {
                if (constraints.oGet(i).name.Icmp(name) == 0) {
//			delete constraints.oGet(i);
                    constraints.RemoveIndex(i);
                    return;
                }
            }
        }
// 

        public static int ContentsFromString(final String str) throws idException {
            int c;
            idToken token = new idToken();
            idLexer src = new idLexer(str, str.length(), "idDeclAF::ContentsFromString");

            c = 0;
            while (src.ReadToken(token)) {
                if (token.Icmp("none") == 0) {
                    c = 0;
                } else if (token.Icmp("solid") == 0) {
                    c |= CONTENTS_SOLID;
                } else if (token.Icmp("body") == 0) {
                    c |= CONTENTS_BODY;
                } else if (token.Icmp("corpse") == 0) {
                    c |= CONTENTS_CORPSE;
                } else if (token.Icmp("playerclip") == 0) {
                    c |= CONTENTS_PLAYERCLIP;
                } else if (token.Icmp("monsterclip") == 0) {
                    c |= CONTENTS_MONSTERCLIP;
                } else if (token.equals(",")) {
                    continue;
                } else {
                    return c;
                }
            }
            return c;
        }

        public static String ContentsToString(final int contents, idStr str) {
            str.oSet("");
            if ((contents & CONTENTS_SOLID) != 0) {
                if (str.Length() != 0) {
                    str.Append(", ");
                }
                str.Append("solid");
            }
            if ((contents & CONTENTS_BODY) != 0) {
                if (str.Length() != 0) {
                    str.Append(", ");
                }
                str.Append("body");
            }
            if ((contents & CONTENTS_CORPSE) != 0) {
                if (str.Length() != 0) {
                    str.Append(", ");
                }
                str.Append("corpse");
            }
            if ((contents & CONTENTS_PLAYERCLIP) != 0) {
                if (str.Length() != 0) {
                    str.Append(", ");
                }
                str.Append("playerclip");
            }
            if ((contents & CONTENTS_MONSTERCLIP) != 0) {
                if (str.Length() != 0) {
                    str.Append(", ");
                }
                str.Append("monsterclip");
            }
            if (str.IsEmpty()) {
                str.oSet("none");
            }
            return str.getData();
        }
// 

        public static declAFJointMod_t JointModFromString(final String str) {
            if (idStr.Icmp(str, "orientation") == 0) {
                return DECLAF_JOINTMOD_AXIS;
            }
            if (idStr.Icmp(str, "position") == 0) {
                return DECLAF_JOINTMOD_ORIGIN;
            }
            if (idStr.Icmp(str, "both") == 0) {
                return DECLAF_JOINTMOD_BOTH;
            }
            return DECLAF_JOINTMOD_AXIS;
        }

        public static String JointModToString(declAFJointMod_t jointMod) {
            switch (jointMod) {
                case DECLAF_JOINTMOD_AXIS: {
                    return "orientation";
                }
                case DECLAF_JOINTMOD_ORIGIN: {
                    return "position";
                }
                case DECLAF_JOINTMOD_BOTH: {
                    return "both";
                }
            }
            return "orientation";
        }

        private boolean ParseContents(idLexer src, int[] c) throws idException {
            idToken token = new idToken();
            idStr str = new idStr();

            while (src.ReadToken(token)) {
                str.Append(token);
                if (!src.CheckTokenString(",")) {
                    break;
                }
                str.Append(",");
            }
            c[0] = ContentsFromString(str.getData());
            return true;
        }

        private boolean ParseBody(idLexer src) throws idException {
            boolean hasJoint = false;
            idToken token = new idToken();
            idAFVector angles = new idAFVector();
            idDeclAF_Body body;// = new idDeclAF_Body();

            body = bodies.Alloc();

            body.SetDefault(this);

            if (0 == src.ExpectTokenType(TT_STRING, 0, token)
                    || !src.ExpectTokenString("{")) {
                return false;
            }

            body.name.oSet(token);
            if (0 == body.name.Icmp("origin") || 0 == body.name.Icmp("world")) {
                src.Error("a body may not be named \"origin\" or \"world\"");
                return false;
            }

            while (src.ReadToken(token)) {

                if (0 == token.Icmp("model")) {
                    if (0 == src.ExpectTokenType(TT_NAME, 0, token)) {
                        return false;
                    }
                    if (0 == token.Icmp("box")) {
                        body.modelType = TRM_BOX;
                        if (!src.ExpectTokenString("(")
                                || !body.v1.Parse(src)
                                || !src.ExpectTokenString(",")
                                || !body.v2.Parse(src)
                                || !src.ExpectTokenString(")")) {
                            return false;
                        }
                    } else if (0 == token.Icmp("octahedron")) {
                        body.modelType = TRM_OCTAHEDRON;
                        if (!src.ExpectTokenString("(")
                                || !body.v1.Parse(src)
                                || !src.ExpectTokenString(",")
                                || !body.v2.Parse(src)
                                || !src.ExpectTokenString(")")) {
                            return false;
                        }
                    } else if (0 == token.Icmp("dodecahedron")) {
                        body.modelType = TRM_DODECAHEDRON;
                        if (!src.ExpectTokenString("(")
                                || !body.v1.Parse(src)
                                || !src.ExpectTokenString(",")
                                || !body.v2.Parse(src)
                                || !src.ExpectTokenString(")")) {
                            return false;
                        }
                    } else if (0 == token.Icmp("cylinder")) {
                        body.modelType = TRM_CYLINDER;
                        if (!src.ExpectTokenString("(")
                                || !body.v1.Parse(src)
                                || !src.ExpectTokenString(",")
                                || !body.v2.Parse(src)
                                || !src.ExpectTokenString(",")) {
                            return false;
                        }
                        body.numSides = src.ParseInt();
                        if (!src.ExpectTokenString(")")) {
                            return false;
                        }
                    } else if (0 == token.Icmp("cone")) {
                        body.modelType = TRM_CONE;
                        if (!src.ExpectTokenString("(")
                                || !body.v1.Parse(src)
                                || !src.ExpectTokenString(",")
                                || !body.v2.Parse(src)
                                || !src.ExpectTokenString(",")) {
                            return false;
                        }
                        body.numSides = src.ParseInt();
                        if (!src.ExpectTokenString(")")) {
                            return false;
                        }
                    } else if (0 == token.Icmp("bone")) {
                        body.modelType = TRM_BONE;
                        if (!src.ExpectTokenString("(")
                                || !body.v1.Parse(src)
                                || !src.ExpectTokenString(",")
                                || !body.v2.Parse(src)
                                || !src.ExpectTokenString(",")) {
                            return false;
                        }
                        body.width = src.ParseFloat();
                        if (!src.ExpectTokenString(")")) {
                            return false;
                        }
                    } else if (0 == token.Icmp("custom")) {
                        src.Error("custom models not yet implemented");
                        return false;
                    } else {
                        src.Error("unkown model type %s", token.getData());
                        return false;
                    }
                } else if (0 == token.Icmp("origin")) {
                    if (!body.origin.Parse(src)) {
                        return false;
                    }
                } else if (0 == token.Icmp("angles")) {
                    if (!angles.Parse(src)) {
                        return false;
                    }
                    body.angles = new idAngles(angles.ToVec3().x, angles.ToVec3().y, angles.ToVec3().z);
                } else if (0 == token.Icmp("joint")) {
                    if (0 == src.ExpectTokenType(TT_STRING, 0, token)) {
                        return false;
                    }
                    body.jointName.oSet(token);
                    hasJoint = true;
                } else if (0 == token.Icmp("mod")) {
                    if (!src.ExpectAnyToken(token)) {
                        return false;
                    }
                    body.jointMod = JointModFromString(token.getData());
                } else if (0 == token.Icmp("density")) {
                    body.density = src.ParseFloat();
                } else if (0 == token.Icmp("inertiaScale")) {
                    src.Parse1DMatrix(9, body.inertiaScale);
                } else if (0 == token.Icmp("friction")) {
                    body.linearFriction = src.ParseFloat();
                    src.ExpectTokenString(",");
                    body.angularFriction = src.ParseFloat();
                    src.ExpectTokenString(",");
                    body.contactFriction = src.ParseFloat();
                } else if (0 == token.Icmp("contents")) {
                    ParseContents(src, body.contents);
                } else if (0 == token.Icmp("clipMask")) {
                    ParseContents(src, body.clipMask);
                } else if (0 == token.Icmp("selfCollision")) {
                    body.selfCollision = src.ParseBool();
                } else if (0 == token.Icmp("containedjoints")) {
                    if (0 == src.ExpectTokenType(TT_STRING, 0, token)) {
                        return false;
                    }
                    body.containedJoints.oSet(token);
                } else if (0 == token.Icmp("frictionDirection")) {
                    if (!body.frictionDirection.Parse(src)) {
                        return false;
                    }
                } else if (0 == token.Icmp("contactMotorDirection")) {
                    if (!body.contactMotorDirection.Parse(src)) {
                        return false;
                    }
                } else if (token.equals("}")) {
                    break;
                } else {
                    src.Error("unknown token %s in body", token.getData());
                    return false;
                }
            }

            if (body.modelType == TRM_INVALID) {
                src.Error("no model set for body");
                return false;
            }

            if (!hasJoint) {
                src.Error("no joint set for body");
                return false;
            }

            body.clipMask[0] |= CONTENTS_MOVEABLECLIP;

            return true;
        }

        private boolean ParseFixed(idLexer src) throws idException {
            idToken token = new idToken();
            idDeclAF_Constraint constraint;

            constraint = constraints.Alloc();
            constraint.SetDefault(this);//TODO:make sure this order is correct.

            if (0 == src.ExpectTokenType(TT_STRING, 0, token)
                    || !src.ExpectTokenString("{")) {
                return false;
            }

            constraint.type = DECLAF_CONSTRAINT_FIXED;
            constraint.name.oSet(token);

            while (src.ReadToken(token)) {

                if (0 == token.Icmp("body1")) {
                    src.ExpectTokenType(TT_STRING, 0, token);
                    constraint.body1.oSet(token);
                } else if (0 == token.Icmp("body2")) {
                    src.ExpectTokenType(TT_STRING, 0, token);
                    constraint.body2.oSet(token);
                } else if (token.equals("}")) {
                    break;
                } else {
                    src.Error("unknown token %s in ball and socket joint", token.getData());
                    return false;
                }
            }

            return true;
        }

        private boolean ParseBallAndSocketJoint(idLexer src) throws idException {
            idToken token = new idToken();
            idDeclAF_Constraint constraint;//= new idDeclAF_Constraint();

            constraint = constraints.Alloc();
            constraint.SetDefault(this);

            if (0 == src.ExpectTokenType(TT_STRING, 0, token)
                    || !src.ExpectTokenString("{")) {
                return false;
            }

            constraint.type = DECLAF_CONSTRAINT_BALLANDSOCKETJOINT;
            constraint.limit = idDeclAF_Constraint.LIMIT_NONE;
            constraint.name.oSet(token);
            constraint.friction = 0.5f;
            constraint.anchor.ToVec3().Zero();
            constraint.shaft[0].ToVec3().Zero();

            while (src.ReadToken(token)) {

                if (0 == token.Icmp("body1")) {
                    src.ExpectTokenType(TT_STRING, 0, token);
                    constraint.body1.oSet(token);
                } else if (0 == token.Icmp("body2")) {
                    src.ExpectTokenType(TT_STRING, 0, token);
                    constraint.body2.oSet(token);
                } else if (0 == token.Icmp("anchor")) {
                    if (!constraint.anchor.Parse(src)) {
                        return false;
                    }
                } else if (0 == token.Icmp("conelimit")) {
                    if (!constraint.limitAxis.Parse(src)
                            || !src.ExpectTokenString(",")) {
                        return false;
                    }
                    constraint.limitAngles[0] = src.ParseFloat();
                    if (!src.ExpectTokenString(",")
                            || !constraint.shaft[0].Parse(src)) {
                        return false;
                    }
                    constraint.limit = idDeclAF_Constraint.LIMIT_CONE;
                } else if (0 == token.Icmp("pyramidlimit")) {
                    if (!constraint.limitAxis.Parse(src)
                            || !src.ExpectTokenString(",")) {
                        return false;
                    }
                    constraint.limitAngles[0] = src.ParseFloat();
                    if (!src.ExpectTokenString(",")) {
                        return false;
                    }
                    constraint.limitAngles[1] = src.ParseFloat();
                    if (!src.ExpectTokenString(",")) {
                        return false;
                    }
                    constraint.limitAngles[2] = src.ParseFloat();
                    if (!src.ExpectTokenString(",")
                            || !constraint.shaft[0].Parse(src)) {
                        return false;
                    }
                    constraint.limit = idDeclAF_Constraint.LIMIT_PYRAMID;
                } else if (0 == token.Icmp("friction")) {
                    constraint.friction = src.ParseFloat();
                } else if (token.equals("}")) {
                    break;
                } else {
                    src.Error("unknown token %s in ball and socket joint", token.getData());
                    return false;
                }
            }

            return true;
        }

        private boolean ParseUniversalJoint(idLexer src) throws idException {
            idToken token = new idToken();
            idDeclAF_Constraint constraint;// = new idDeclAF_Constraint;

            constraint = constraints.Alloc();
            constraint.SetDefault(this);

            if (0 == src.ExpectTokenType(TT_STRING, 0, token)
                    || !src.ExpectTokenString("{")) {
                return false;
            }

            constraint.type = DECLAF_CONSTRAINT_UNIVERSALJOINT;
            constraint.limit = idDeclAF_Constraint.LIMIT_NONE;
            constraint.name.oSet(token);
            constraint.friction = 0.5f;
            constraint.anchor.ToVec3().Zero();
            constraint.shaft[0].ToVec3().Zero();
            constraint.shaft[1].ToVec3().Zero();

            while (src.ReadToken(token)) {

                if (0 == token.Icmp("body1")) {
                    src.ExpectTokenType(TT_STRING, 0, token);
                    constraint.body1.oSet(token);
                } else if (0 == token.Icmp("body2")) {
                    src.ExpectTokenType(TT_STRING, 0, token);
                    constraint.body2.oSet(token);
                } else if (0 == token.Icmp("anchor")) {
                    if (!constraint.anchor.Parse(src)) {
                        return false;
                    }
                } else if (0 == token.Icmp("shafts")) {
                    if (!constraint.shaft[0].Parse(src)
                            || !src.ExpectTokenString(",")
                            || !constraint.shaft[1].Parse(src)) {
                        return false;
                    }
                } else if (0 == token.Icmp("conelimit")) {
                    if (!constraint.limitAxis.Parse(src)
                            || !src.ExpectTokenString(",")) {
                        return false;
                    }
                    constraint.limitAngles[0] = src.ParseFloat();
                    constraint.limit = idDeclAF_Constraint.LIMIT_CONE;
                } else if (0 == token.Icmp("pyramidlimit")) {
                    if (!constraint.limitAxis.Parse(src)
                            || !src.ExpectTokenString(",")) {
                        return false;
                    }
                    constraint.limitAngles[0] = src.ParseFloat();
                    if (!src.ExpectTokenString(",")) {
                        return false;
                    }
                    constraint.limitAngles[1] = src.ParseFloat();
                    if (!src.ExpectTokenString(",")) {
                        return false;
                    }
                    constraint.limitAngles[2] = src.ParseFloat();
                    constraint.limit = idDeclAF_Constraint.LIMIT_PYRAMID;
                } else if (0 == token.Icmp("friction")) {
                    constraint.friction = src.ParseFloat();
                } else if (token.equals("}")) {
                    break;
                } else {
                    src.Error("unknown token %s in universal joint", token.getData());
                    return false;
                }
            }

            return true;
        }

        private boolean ParseHinge(idLexer src) throws idException {
            idToken token = new idToken();
            idDeclAF_Constraint constraint;// = new idDeclAF_Constraint;

            constraint = constraints.Alloc();
            constraint.SetDefault(this);

            if (0 == src.ExpectTokenType(TT_STRING, 0, token)
                    || !src.ExpectTokenString("{")) {
                return false;
            }

            constraint.type = DECLAF_CONSTRAINT_HINGE;
            constraint.limit = idDeclAF_Constraint.LIMIT_NONE;
            constraint.name.oSet(token);
            constraint.friction = 0.5f;
            constraint.anchor.ToVec3().Zero();
            constraint.axis.ToVec3().Zero();

            while (src.ReadToken(token)) {

                if (0 == token.Icmp("body1")) {
                    src.ExpectTokenType(TT_STRING, 0, token);
                    constraint.body1.oSet(token);
                } else if (0 == token.Icmp("body2")) {
                    src.ExpectTokenType(TT_STRING, 0, token);
                    constraint.body2.oSet(token);
                } else if (0 == token.Icmp("anchor")) {
                    if (!constraint.anchor.Parse(src)) {
                        return false;
                    }
                } else if (0 == token.Icmp("axis")) {
                    if (!constraint.axis.Parse(src)) {
                        return false;
                    }
                } else if (0 == token.Icmp("limit")) {
                    constraint.limitAngles[0] = src.ParseFloat();
                    if (!src.ExpectTokenString(",")) {
                        return false;
                    }
                    constraint.limitAngles[1] = src.ParseFloat();
                    if (!src.ExpectTokenString(",")) {
                        return false;
                    }
                    constraint.limitAngles[2] = src.ParseFloat();
                    constraint.limit = idDeclAF_Constraint.LIMIT_CONE;
                } else if (0 == token.Icmp("friction")) {
                    constraint.friction = src.ParseFloat();
                } else if (token.equals("}")) {
                    break;
                } else {
                    src.Error("unknown token %s in hinge", token.getData());
                    return false;
                }
            }

            return true;
        }

        private boolean ParseSlider(idLexer src) throws idException {
            idToken token = new idToken();
            idDeclAF_Constraint constraint;// = new idDeclAF_Constraint;

            constraint = constraints.Alloc();
            constraint.SetDefault(this);

            if (0 == src.ExpectTokenType(TT_STRING, 0, token)
                    || !src.ExpectTokenString("{")) {
                return false;
            }

            constraint.type = DECLAF_CONSTRAINT_SLIDER;
            constraint.limit = idDeclAF_Constraint.LIMIT_NONE;
            constraint.name.oSet(token);
            constraint.friction = 0.5f;

            while (src.ReadToken(token)) {

                if (0 == token.Icmp("body1")) {
                    src.ExpectTokenType(TT_STRING, 0, token);
                    constraint.body1.oSet(token);
                } else if (0 == token.Icmp("body2")) {
                    src.ExpectTokenType(TT_STRING, 0, token);
                    constraint.body2.oSet(token);
                } else if (0 == token.Icmp("axis")) {
                    if (!constraint.axis.Parse(src)) {
                        return false;
                    }
                } else if (0 == token.Icmp("friction")) {
                    constraint.friction = src.ParseFloat();
                } else if (token.equals("}")) {
                    break;
                } else {
                    src.Error("unknown token %s in slider", token.getData());
                    return false;
                }
            }

            return true;
        }

        private boolean ParseSpring(idLexer src) throws idException {
            idToken token = new idToken();
            idDeclAF_Constraint constraint;// = new idDeclAF_Constraint;

            constraint = constraints.Alloc();
            constraint.SetDefault(this);

            if (0 == src.ExpectTokenType(TT_STRING, 0, token)
                    || !src.ExpectTokenString("{")) {
                return false;
            }

            constraint.type = DECLAF_CONSTRAINT_SPRING;
            constraint.limit = idDeclAF_Constraint.LIMIT_NONE;
            constraint.name.oSet(token);
            constraint.friction = 0.5f;

            while (src.ReadToken(token)) {

                if (0 == token.Icmp("body1")) {
                    src.ExpectTokenType(TT_STRING, 0, token);
                    constraint.body1.oSet(token);
                } else if (0 == token.Icmp("body2")) {
                    src.ExpectTokenType(TT_STRING, 0, token);
                    constraint.body2.oSet(token);
                } else if (0 == token.Icmp("anchor1")) {
                    if (!constraint.anchor.Parse(src)) {
                        return false;
                    }
                } else if (0 == token.Icmp("anchor2")) {
                    if (!constraint.anchor2.Parse(src)) {
                        return false;
                    }
                } else if (0 == token.Icmp("friction")) {
                    constraint.friction = src.ParseFloat();
                } else if (0 == token.Icmp("stretch")) {
                    constraint.stretch = src.ParseFloat();
                } else if (0 == token.Icmp("compress")) {
                    constraint.compress = src.ParseFloat();
                } else if (0 == token.Icmp("damping")) {
                    constraint.damping = src.ParseFloat();
                } else if (0 == token.Icmp("restLength")) {
                    constraint.restLength = src.ParseFloat();
                } else if (0 == token.Icmp("minLength")) {
                    constraint.minLength = src.ParseFloat();
                } else if (0 == token.Icmp("maxLength")) {
                    constraint.maxLength = src.ParseFloat();
                } else if (token.equals("}")) {
                    break;
                } else {
                    src.Error("unknown token %s in spring", token.getData());
                    return false;
                }
            }

            return true;
        }

        private boolean ParseSettings(idLexer src) throws idException {
            idToken token = new idToken();

            if (!src.ExpectTokenString("{")) {
                return false;
            }

            while (src.ReadToken(token)) {

                if (0 == token.Icmp("mesh")) {
                    if (0 == src.ExpectTokenType(TT_STRING, 0, token)) {
                        return false;
                    }
                } else if (0 == token.Icmp("anim")) {
                    if (0 == src.ExpectTokenType(TT_STRING, 0, token)) {
                        return false;
                    }
                } else if (0 == token.Icmp("model")) {
                    if (0 == src.ExpectTokenType(TT_STRING, 0, token)) {
                        return false;
                    }
                    model = token;
                } else if (0 == token.Icmp("skin")) {
                    if (0 == src.ExpectTokenType(TT_STRING, 0, token)) {
                        return false;
                    }
                    skin = token;
                } else if (0 == token.Icmp("friction")) {

                    defaultLinearFriction = src.ParseFloat();
                    if (!src.ExpectTokenString(",")) {
                        return false;
                    }
                    defaultAngularFriction = src.ParseFloat();
                    if (!src.ExpectTokenString(",")) {
                        return false;
                    }
                    defaultContactFriction = src.ParseFloat();
                    if (src.CheckTokenString(",")) {
                        defaultConstraintFriction = src.ParseFloat();
                    }
                } else if (0 == token.Icmp("totalMass")) {
                    totalMass = src.ParseFloat();
                } else if (0 == token.Icmp("suspendSpeed")) {

                    suspendVelocity.oSet(0, src.ParseFloat());
                    if (!src.ExpectTokenString(",")) {
                        return false;
                    }
                    suspendVelocity.oSet(1, src.ParseFloat());
                    if (!src.ExpectTokenString(",")) {
                        return false;
                    }
                    suspendAcceleration.oSet(0, src.ParseFloat());
                    if (!src.ExpectTokenString(",")) {
                        return false;
                    }
                    suspendAcceleration.oSet(1, src.ParseFloat());
                } else if (0 == token.Icmp("noMoveTime")) {
                    noMoveTime = src.ParseFloat();
                } else if (0 == token.Icmp("noMoveTranslation")) {
                    noMoveTranslation = src.ParseFloat();
                } else if (0 == token.Icmp("noMoveRotation")) {
                    noMoveRotation = src.ParseFloat();
                } else if (0 == token.Icmp("minMoveTime")) {
                    minMoveTime = src.ParseFloat();
                } else if (0 == token.Icmp("maxMoveTime")) {
                    maxMoveTime = src.ParseFloat();
                } else if (0 == token.Icmp("contents")) {
                    ParseContents(src, contents);
                } else if (0 == token.Icmp("clipMask")) {
                    ParseContents(src, clipMask);
                } else if (0 == token.Icmp("selfCollision")) {
                    selfCollision = src.ParseBool();
                } else if (token.equals("}")) {
                    break;
                } else {
                    src.Error("unknown token %s in settings", token.getData());
                    return false;
                }
            }

            return true;
        }
//

        private boolean WriteBody(idFile f, final idDeclAF_Body body) {
            idStr str = new idStr();

            f.WriteFloatString("\nbody \"%s\" {\n", body.name.getData());
            f.WriteFloatString("\tjoint \"%s\"\n", body.jointName.getData());
            f.WriteFloatString("\tmod %s\n", JointModToString(body.jointMod));
            switch (body.modelType) {
                case TRM_BOX: {
                    f.WriteFloatString("\tmodel box( ");
                    body.v1.Write(f);
                    f.WriteFloatString(", ");
                    body.v2.Write(f);
                    f.WriteFloatString(" )\n");
                    break;
                }
                case TRM_OCTAHEDRON: {
                    f.WriteFloatString("\tmodel octahedron( ");
                    body.v1.Write(f);
                    f.WriteFloatString(", ");
                    body.v2.Write(f);
                    f.WriteFloatString(" )\n");
                    break;
                }
                case TRM_DODECAHEDRON: {
                    f.WriteFloatString("\tmodel dodecahedron( ");
                    body.v1.Write(f);
                    f.WriteFloatString(", ");
                    body.v2.Write(f);
                    f.WriteFloatString(" )\n");
                    break;
                }
                case TRM_CYLINDER: {
                    f.WriteFloatString("\tmodel cylinder( ");
                    body.v1.Write(f);
                    f.WriteFloatString(", ");
                    body.v2.Write(f);
                    f.WriteFloatString(", %d )\n", body.numSides);
                    break;
                }
                case TRM_CONE: {
                    f.WriteFloatString("\tmodel cone( ");
                    body.v1.Write(f);
                    f.WriteFloatString(", ");
                    body.v2.Write(f);
                    f.WriteFloatString(", %d )\n", body.numSides);
                    break;
                }
                case TRM_BONE: {
                    f.WriteFloatString("\tmodel bone( ");
                    body.v1.Write(f);
                    f.WriteFloatString(", ");
                    body.v2.Write(f);
                    f.WriteFloatString(", %f )\n", body.width);
                    break;
                }
                default:
                    assert (false);
                    break;
            }
            f.WriteFloatString("\torigin ");
            body.origin.Write(f);
            f.WriteFloatString("\n");
            if (body.angles != Angles.getAng_zero()) {
                f.WriteFloatString("\tangles ( %f, %f, %f )\n", body.angles.pitch, body.angles.yaw, body.angles.roll);
            }
            f.WriteFloatString("\tdensity %f\n", body.density);
            if (!body.inertiaScale.equals(idMat3.getMat3_identity())) {
                final idMat3 ic = body.inertiaScale;
                f.WriteFloatString("\tinertiaScale (%f %f %f %f %f %f %f %f %f)\n",
                        ic.oGet(0).oGet(0), ic.oGet(0).oGet(1), ic.oGet(0).oGet(2),
                        ic.oGet(1).oGet(0), ic.oGet(1).oGet(1), ic.oGet(1).oGet(2),
                        ic.oGet(2).oGet(0), ic.oGet(2).oGet(1), ic.oGet(2).oGet(2));
            }
            if (body.linearFriction != -1) {
                f.WriteFloatString("\tfriction %f, %f, %f\n", body.linearFriction, body.angularFriction, body.contactFriction);
            }
            f.WriteFloatString("\tcontents %s\n", ContentsToString(body.contents[0], str));
            f.WriteFloatString("\tclipMask %s\n", ContentsToString(body.clipMask[0], str));
            f.WriteFloatString("\tselfCollision %d\n", body.selfCollision);
            if (body.frictionDirection.ToVec3() != Vector.getVec3_origin()) {
                f.WriteFloatString("\tfrictionDirection ");
                body.frictionDirection.Write(f);
                f.WriteFloatString("\n");
            }
            if (body.contactMotorDirection.ToVec3() != Vector.getVec3_origin()) {
                f.WriteFloatString("\tcontactMotorDirection ");
                body.contactMotorDirection.Write(f);
                f.WriteFloatString("\n");
            }
            f.WriteFloatString("\tcontainedJoints \"%s\"\n", body.containedJoints.getData());
            f.WriteFloatString("}\n");
            return true;
        }

        private boolean WriteFixed(idFile f, final idDeclAF_Constraint c) {
            f.WriteFloatString("\nfixed \"%s\" {\n", c.name);
            f.WriteFloatString("\tbody1 \"%s\"\n", c.body1);
            f.WriteFloatString("\tbody2 \"%s\"\n", c.body2);
            f.WriteFloatString("}\n");
            return true;
        }

        private boolean WriteBallAndSocketJoint(idFile f, final idDeclAF_Constraint c) {
            f.WriteFloatString("\nballAndSocketJoint \"%s\" {\n", c.name);
            f.WriteFloatString("\tbody1 \"%s\"\n", c.body1);
            f.WriteFloatString("\tbody2 \"%s\"\n", c.body2);
            f.WriteFloatString("\tanchor ");
            c.anchor.Write(f);
            f.WriteFloatString("\n");
            f.WriteFloatString("\tfriction %f\n", c.friction);
            if (c.limit == idDeclAF_Constraint.LIMIT_CONE) {
                f.WriteFloatString("\tconeLimit ");
                c.limitAxis.Write(f);
                f.WriteFloatString(", %f, ", c.limitAngles[0]);
                c.shaft[0].Write(f);
                f.WriteFloatString("\n");
            } else if (c.limit == idDeclAF_Constraint.LIMIT_PYRAMID) {
                f.WriteFloatString("\tpyramidLimit ");
                c.limitAxis.Write(f);
                f.WriteFloatString(", %f, %f, %f, ", c.limitAngles[0], c.limitAngles[1], c.limitAngles[2]);
                c.shaft[0].Write(f);
                f.WriteFloatString("\n");
            }
            f.WriteFloatString("}\n");
            return true;
        }

        private boolean WriteUniversalJoint(idFile f, final idDeclAF_Constraint c) {
            f.WriteFloatString("\nuniversalJoint \"%s\" {\n", c.name);
            f.WriteFloatString("\tbody1 \"%s\"\n", c.body1);
            f.WriteFloatString("\tbody2 \"%s\"\n", c.body2);
            f.WriteFloatString("\tanchor ");
            c.anchor.Write(f);
            f.WriteFloatString("\n");
            f.WriteFloatString("\tshafts ");
            c.shaft[0].Write(f);
            f.WriteFloatString(", ");
            c.shaft[1].Write(f);
            f.WriteFloatString("\n");
            f.WriteFloatString("\tfriction %f\n", c.friction);
            if (c.limit == idDeclAF_Constraint.LIMIT_CONE) {
                f.WriteFloatString("\tconeLimit ");
                c.limitAxis.Write(f);
                f.WriteFloatString(", %f\n", c.limitAngles[0]);
            } else if (c.limit == idDeclAF_Constraint.LIMIT_PYRAMID) {
                f.WriteFloatString("\tpyramidLimit ");
                c.limitAxis.Write(f);
                f.WriteFloatString(", %f, %f, %f\n", c.limitAngles[0], c.limitAngles[1], c.limitAngles[2]);
            }
            f.WriteFloatString("}\n");
            return true;
        }

        private boolean WriteHinge(idFile f, final idDeclAF_Constraint c) {
            f.WriteFloatString("\nhinge \"%s\" {\n", c.name);
            f.WriteFloatString("\tbody1 \"%s\"\n", c.body1);
            f.WriteFloatString("\tbody2 \"%s\"\n", c.body2);
            f.WriteFloatString("\tanchor ");
            c.anchor.Write(f);
            f.WriteFloatString("\n");
            f.WriteFloatString("\taxis ");
            c.axis.Write(f);
            f.WriteFloatString("\n");
            f.WriteFloatString("\tfriction %f\n", c.friction);
            if (c.limit == idDeclAF_Constraint.LIMIT_CONE) {
                f.WriteFloatString("\tlimit ");
                f.WriteFloatString("%f, %f, %f", c.limitAngles[0], c.limitAngles[1], c.limitAngles[2]);
                f.WriteFloatString("\n");
            }
            f.WriteFloatString("}\n");
            return true;
        }

        private boolean WriteSlider(idFile f, final idDeclAF_Constraint c) {
            f.WriteFloatString("\nslider \"%s\" {\n", c.name);
            f.WriteFloatString("\tbody1 \"%s\"\n", c.body1);
            f.WriteFloatString("\tbody2 \"%s\"\n", c.body2);
            f.WriteFloatString("\taxis ");
            c.axis.Write(f);
            f.WriteFloatString("\n");
            f.WriteFloatString("\tfriction %f\n", c.friction);
            f.WriteFloatString("}\n");
            return true;
        }

        private boolean WriteSpring(idFile f, final idDeclAF_Constraint c) {
            f.WriteFloatString("\nspring \"%s\" {\n", c.name);
            f.WriteFloatString("\tbody1 \"%s\"\n", c.body1);
            f.WriteFloatString("\tbody2 \"%s\"\n", c.body2);
            f.WriteFloatString("\tanchor1 ");
            c.anchor.Write(f);
            f.WriteFloatString("\n");
            f.WriteFloatString("\tanchor2 ");
            c.anchor2.Write(f);
            f.WriteFloatString("\n");
            f.WriteFloatString("\tfriction %f\n", c.friction);
            f.WriteFloatString("\tstretch %f\n", c.stretch);
            f.WriteFloatString("\tcompress %f\n", c.compress);
            f.WriteFloatString("\tdamping %f\n", c.damping);
            f.WriteFloatString("\trestLength %f\n", c.restLength);
            f.WriteFloatString("\tminLength %f\n", c.minLength);
            f.WriteFloatString("\tmaxLength %f\n", c.maxLength);
            f.WriteFloatString("}\n");
            return true;
        }

        private boolean WriteConstraint(idFile f, final idDeclAF_Constraint c) {
            switch (c.type) {
                case DECLAF_CONSTRAINT_FIXED:
                    return WriteFixed(f, c);
                case DECLAF_CONSTRAINT_BALLANDSOCKETJOINT:
                    return WriteBallAndSocketJoint(f, c);
                case DECLAF_CONSTRAINT_UNIVERSALJOINT:
                    return WriteUniversalJoint(f, c);
                case DECLAF_CONSTRAINT_HINGE:
                    return WriteHinge(f, c);
                case DECLAF_CONSTRAINT_SLIDER:
                    return WriteSlider(f, c);
                case DECLAF_CONSTRAINT_SPRING:
                    return WriteSpring(f, c);
                default:
                    break;
            }
            return false;
        }

        private boolean WriteSettings(idFile f) {
            idStr str = new idStr();

            f.WriteFloatString("\nsettings {\n");
            f.WriteFloatString("\tmodel \"%s\"\n", model);
            f.WriteFloatString("\tskin \"%s\"\n", skin);
            f.WriteFloatString("\tfriction %f, %f, %f, %f\n", defaultLinearFriction, defaultAngularFriction, defaultContactFriction, defaultConstraintFriction);
            f.WriteFloatString("\tsuspendSpeed %f, %f, %f, %f\n", suspendVelocity.oGet(0), suspendVelocity.oGet(1), suspendAcceleration.oGet(0), suspendAcceleration.oGet(1));
            f.WriteFloatString("\tnoMoveTime %f\n", noMoveTime);
            f.WriteFloatString("\tnoMoveTranslation %f\n", noMoveTranslation);
            f.WriteFloatString("\tnoMoveRotation %f\n", noMoveRotation);
            f.WriteFloatString("\tminMoveTime %f\n", minMoveTime);
            f.WriteFloatString("\tmaxMoveTime %f\n", maxMoveTime);
            f.WriteFloatString("\ttotalMass %f\n", totalMass);
            f.WriteFloatString("\tcontents %s\n", ContentsToString(contents[0], str));
            f.WriteFloatString("\tclipMask %s\n", ContentsToString(clipMask[0], str));
            f.WriteFloatString("\tselfCollision %d\n", selfCollision);
            f.WriteFloatString("}\n");
            return true;
        }
//

        private boolean RebuildTextSource() {
            int i;
            idFile_Memory f = new idFile_Memory();

            f.WriteFloatString("\n\n/*\n"
                    + "\tGenerated by the Articulated Figure Editor.\n"
                    + "\tDo not edit directly but launch the game and type 'editAFs' on the console.\n"
                    + "*/\n");

            f.WriteFloatString("\narticulatedFigure %s {\n", GetName());

            if (!WriteSettings(f)) {
                return false;
            }

            for (i = 0; i < bodies.Num(); i++) {
                if (!WriteBody(f, bodies.oGet(i))) {
                    return false;
                }
            }

            for (i = 0; i < constraints.Num(); i++) {
                if (!WriteConstraint(f, constraints.oGet(i))) {
                    return false;
                }
            }

            f.WriteFloatString("\n}");

            SetText(new String(f.GetDataPtr().array()));

            return true;
        }
    };
}
