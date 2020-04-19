package neo.Game;

import static neo.Game.Entity.EV_Activate;
import static neo.Game.Entity.TH_THINK;
import static neo.Game.GameSys.SysCvar.g_debugCinematic;
import static neo.Game.GameSys.SysCvar.g_showcamerainfo;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Script.Script_Thread.EV_Thread_SetCallback;
import static neo.Renderer.Model.MD5_CAMERA_EXT;
import static neo.Renderer.Model.MD5_VERSION;
import static neo.Renderer.Model.MD5_VERSION_STRING;
import static neo.framework.UsercmdGen.USERCMD_HZ;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWPATHNAMES;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGCONCAT;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGESCAPECHARS;
import static neo.idlib.Text.Str.va;

import java.util.HashMap;
import java.util.Map;

import neo.Game.Entity.idEntity;
import neo.Game.Game_local.idEntityPtr;
import neo.Game.Game_local.idGameLocal;
import neo.Game.GameSys.Class;
import neo.Game.GameSys.Class.eventCallback_t;
import neo.Game.GameSys.Class.eventCallback_t0;
import neo.Game.GameSys.Class.eventCallback_t1;
import neo.Game.GameSys.Class.idEventArg;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import neo.Game.Script.Script_Thread.idThread;
import neo.Renderer.RenderWorld.renderView_s;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Quat.idCQuat;
import neo.idlib.math.Quat.idQuat;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Matrix.idMat3;

/**
 *
 */
public class Camera {

    static final idEventDef EV_Camera_SetAttachments = new idEventDef("<getattachments>", null);
    //
    public static final idEventDef EV_Camera_Start = new idEventDef("start", null);
    public static final idEventDef EV_Camera_Stop = new idEventDef("stop", null);

    /*
     ===============================================================================

     Camera providing an alternative view of the level.

     ===============================================================================
     */
    public static abstract class idCamera extends idEntity {
        //public	ABSTRACT_PROTOTYPE( idCamera );

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public abstract void GetViewParms(renderView_s view);

        @Override
        public renderView_s GetRenderView() {
            final renderView_s rv = super.GetRenderView();
            GetViewParms(rv);
            return rv;
        }

        public void Stop() {
        }
    }

    /*
     ===============================================================================

     idCameraView

     ===============================================================================
     */
    public static class idCameraView extends idCamera {
/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		//    public	CLASS_PROTOTYPE( idCameraView );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idCamera.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idCameraView>) idCameraView::Event_Activate);
            eventCallbacks.put(EV_Camera_SetAttachments, (eventCallback_t0<idCameraView>) idCameraView::Event_SetAttachments);
        }

        protected float    fov;
        protected idEntity attachedTo;
        protected idEntity attachedView;
        //
        //

        public idCameraView() {
            this.fov = 90.0f;
            this.attachedTo = null;
            this.attachedView = null;
        }

        // save games
        @Override
        public void Save(idSaveGame savefile) {				// archives object for save game file
            savefile.WriteFloat(this.fov);
            savefile.WriteObject(this.attachedTo);
            savefile.WriteObject(this.attachedView);
        }

        @Override
        public void Restore(idRestoreGame savefile) {				// unarchives object from save game file
            final float[] fov = {this.fov};

            savefile.ReadFloat(fov);
            savefile.ReadObject(this./*reinterpret_cast<idClass *&>(*/attachedTo);
            savefile.ReadObject( this./*reinterpret_cast<idClass *&>(*/attachedView);

            this.fov = fov[0];
        }

        @Override
        public void Spawn() {
            super.Spawn();
            
            // if no target specified use ourself
            final String cam = this.spawnArgs.GetString("cameraTarget");
            if (cam.isEmpty()) {
                this.spawnArgs.Set("cameraTarget", this.spawnArgs.GetString("name"));
            }
            this.fov = this.spawnArgs.GetFloat("fov", "90");

            PostEventMS(EV_Camera_SetAttachments, 0);

            UpdateChangeableSpawnArgs(null);
        }

        @Override
        public void GetViewParms(renderView_s view) {
            assert (view != null);

            if (view == null) {
                return;
            }

            idVec3 dir;
            idEntity ent;

            if (this.attachedTo != null) {
                ent = this.attachedTo;
            } else {
                ent = this;
            }

            view.vieworg = new idVec3(ent.GetPhysics().GetOrigin());
            if (this.attachedView != null) {
                dir = this.attachedView.GetPhysics().GetOrigin().oMinus(view.vieworg);
                dir.Normalize();
                view.viewaxis = dir.ToMat3();
            } else {
                view.viewaxis = new idMat3(ent.GetPhysics().GetAxis());
            }

            {
                final float[] fov_x = {view.fov_x}, fov_y = {view.fov_y};
                gameLocal.CalcFov(this.fov, fov_x, fov_y);
                view.fov_x = fov_x[0];
                view.fov_y = fov_y[0];
            }
        }

        @Override
        public void Stop() {
            if (g_debugCinematic.GetBool()) {
                gameLocal.Printf("%d: '%s' stop\n", gameLocal.framenum, GetName());
            }
            gameLocal.SetCamera(null);
            ActivateTargets(gameLocal.GetLocalPlayer());
        }

        protected void Event_Activate(idEventArg<idEntity> activator) {
            if (this.spawnArgs.GetBool("trigger")) {
                if (gameLocal.GetCamera() != this) {
                    if (g_debugCinematic.GetBool()) {
                        gameLocal.Printf("%d: '%s' start\n", gameLocal.framenum, GetName());
                    }

                    gameLocal.SetCamera(this);
                } else {
                    if (g_debugCinematic.GetBool()) {
                        gameLocal.Printf("%d: '%s' stop\n", gameLocal.framenum, GetName());
                    }
                    gameLocal.SetCamera(null);
                }
            }
        }

        protected void Event_SetAttachments() {
            final idEntity[] attachedTo = {this.attachedTo}, attachedView = {this.attachedView};
            SetAttachment(attachedTo, "attachedTo");
            SetAttachment(attachedView, "attachedView");
            this.attachedTo = attachedTo[0];
            this.attachedView = attachedView[0];
        }

        protected void SetAttachment(idEntity[] e, final String p) {
            final String cam = this.spawnArgs.GetString(p);
            if (!cam.isEmpty()) {
                e[0] = gameLocal.FindEntity(cam);
            }
        }

        @Override
        public Class.idClass CreateInstance() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public java.lang.Class<?> /*idTypeInfo*/ GetType() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public eventCallback_t<?> getEventCallBack(idEventDef event) {
            return eventCallbacks.get(event);
        }

        public static Map<idEventDef, eventCallback_t> getEventCallBacks() {
            return eventCallbacks;
        }

    }

    /*
     ===============================================================================

     A camera which follows a path defined by an animation.

     ===============================================================================
     */
    public static class cameraFrame_t {

        idCQuat q;
        idVec3 t;
        float fov;

        public cameraFrame_t() {
            this.q = new idCQuat();
            this.t = new idVec3();
        }
    }

    public static class idCameraAnim extends idCamera {
/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		//        public 	CLASS_PROTOTYPE( idCameraAnim );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idCamera.getEventCallBacks());
            eventCallbacks.put(EV_Thread_SetCallback, (eventCallback_t0<idCameraAnim>) idCameraAnim::Event_SetCallback);
            eventCallbacks.put(EV_Camera_Stop, (eventCallback_t0<idCameraAnim>) idCameraAnim::Event_Stop);
            eventCallbacks.put(EV_Camera_Start, (eventCallback_t0<idCameraAnim>) idCameraAnim::Event_Start);
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idCameraAnim>) idCameraAnim::Event_Activate);
        }


        private int                   threadNum;
        private idVec3                offset;
        private int                   frameRate;
        private int                   starttime;
        private int                   cycle;
        private final idList<Integer>       cameraCuts;
        private final idList<cameraFrame_t> camera;
        private final idEntityPtr<idEntity> activator;
        //
        //

        public idCameraAnim() {
            this.threadNum = 0;
            this.offset = new idVec3();
            this.frameRate = 0;
            this.starttime = 0;
            this.cycle = 1;
            this.cameraCuts = new idList<>();
            this.camera = new idList<>();
            this.activator = new idEntityPtr<>(null);

        }
        //~idCameraAnim();

        // save games
        @Override
        public void Save(idSaveGame savefile) {				// archives object for save game file
            savefile.WriteInt(this.threadNum);
            savefile.WriteVec3(this.offset);
            savefile.WriteInt(this.frameRate);
            savefile.WriteInt(this.starttime);
            savefile.WriteInt(this.cycle);
            this.activator.Save(savefile);
        }

        @Override
        public void Restore(idRestoreGame savefile) {				// unarchives object from save game file
            this.threadNum = savefile.ReadInt();
            savefile.ReadVec3(this.offset);
            this.frameRate = savefile.ReadInt();
            this.starttime = savefile.ReadInt();
            this.cycle = savefile.ReadInt();
            this.activator.Restore(savefile);

            LoadAnim();
        }

        @Override
        public void Spawn() {
            super.Spawn();
            
            if (this.spawnArgs.GetVector("old_origin", "0 0 0", this.offset)) {
                this.offset = GetPhysics().GetOrigin().oMinus(this.offset);
            } else {
                this.offset.Zero();
            }

            // always think during cinematics
            this.cinematic = true;

            LoadAnim();
        }

        @Override
        public void GetViewParms(renderView_s view) {
            int realFrame;
            int frame;
            int frameTime;
            float lerp;
            float invlerp;
            cameraFrame_t camFrame;
            int i;
            int cut;
            idQuat q1, q2;
			final idQuat q3 = new idQuat();

            assert (view != null);
            if (null == view) {
                return;
            }

            if (this.camera.Num() == 0) {
                // we most likely are in the middle of a restore
                // FIXME: it would be better to fix it so this doesn't get called during a restore
                return;
            }

            if (this.frameRate == USERCMD_HZ) {
                frameTime = gameLocal.time - this.starttime;
                frame = frameTime / idGameLocal.msec;
                lerp = 0.0f;
            } else {
                frameTime = (gameLocal.time - this.starttime) * this.frameRate;
                frame = frameTime / 1000;
                lerp = (frameTime % 1000) * 0.001f;
            }

            // skip any frames where camera cuts occur
            realFrame = frame;
            cut = 0;
            for (i = 0; i < this.cameraCuts.Num(); i++) {
                if (frame < this.cameraCuts.oGet(i)) {
                    break;
                }
                frame++;
                cut++;
            }

            if (g_debugCinematic.GetBool()) {
                final int prevFrameTime = (gameLocal.time - this.starttime - idGameLocal.msec) * this.frameRate;
                int prevFrame = prevFrameTime / 1000;
                int prevCut;

                prevCut = 0;
                for (i = 0; i < this.cameraCuts.Num(); i++) {
                    if (prevFrame < this.cameraCuts.oGet(i)) {
                        break;
                    }
                    prevFrame++;
                    prevCut++;
                }

                if (prevCut != cut) {
                    gameLocal.Printf("%d: '%s' cut %d\n", gameLocal.framenum, GetName(), cut);
                }
            }

            // clamp to the first frame.  also check if this is a one frame anim.  one frame anims would end immediately,
            // but since they're mainly used for static cams anyway, just stay on it infinitely.
            if ((frame < 0) || (this.camera.Num() < 2)) {
                view.viewaxis = this.camera.oGet(0).q.ToQuat().ToMat3();
                view.vieworg = this.camera.oGet(0).t.oPlus(this.offset);
                view.fov_x = this.camera.oGet(0).fov;
            } else if (frame > (this.camera.Num() - 2)) {
                if (this.cycle > 0) {
                    this.cycle--;
                }

                if (this.cycle != 0) {
                    // advance start time so that we loop
                    this.starttime += ((this.camera.Num() - this.cameraCuts.Num()) * 1000) / this.frameRate;
                    GetViewParms(view);
                    return;
                }

                Stop();
                if (gameLocal.GetCamera() != null) {
                    // we activated another camera when we stopped, so get it's viewparms instead
                    gameLocal.GetCamera().GetViewParms(view);
                    return;
                } else {
                    // just use our last frame
                    camFrame = this.camera.oGet(this.camera.Num() - 1);
                    view.viewaxis = camFrame.q.ToQuat().ToMat3();
                    view.vieworg = camFrame.t.oPlus(this.offset);
                    view.fov_x = camFrame.fov;
                }
            } else if (lerp == 0.0f) {
                camFrame = this.camera.oGet(frame);
                view.viewaxis = camFrame/*[ 0 ]*/.q.ToMat3();
                view.vieworg = camFrame/*[ 0 ]*/.t.oPlus(this.offset);
                view.fov_x = camFrame/*[ 0 ]*/.fov;
            } else {
                camFrame = this.camera.oGet(frame);
                final cameraFrame_t nextFrame = this.camera.oGet(frame + 1);
                invlerp = 1.0f - lerp;
                q1 = camFrame/*[ 0 ]*/.q.ToQuat();
                q2 = nextFrame.q.ToQuat();
                q3.Slerp(q1, q2, lerp);
                view.viewaxis = q3.ToMat3();
                view.vieworg = camFrame/*[ 0 ]*/.t.oMultiply(invlerp).oPlus(nextFrame.t.oMultiply(lerp).oPlus(this.offset));
                view.fov_x = (camFrame/*[ 0 ]*/.fov * invlerp) + (nextFrame.fov * lerp);
            }

            {
                final float[] fov_x = {view.fov_x}, fov_y = {view.fov_y};
                gameLocal.CalcFov(view.fov_x, fov_x, fov_y);
                view.fov_x = fov_x[0];
                view.fov_y = fov_y[0];
            }

            // setup the pvs for this frame
            UpdatePVSAreas(view.vieworg);

// if(false){
            // static int lastFrame = 0;
            // static idVec3 lastFrameVec( 0.0f, 0.0f, 0.0f );
            // if ( gameLocal.time != lastFrame ) {
            // gameRenderWorld.DebugBounds( colorCyan, idBounds( view.vieworg ).Expand( 16.0f ), vec3_origin, gameLocal.msec );
            // gameRenderWorld.DebugLine( colorRed, view.vieworg, view.vieworg + idVec3( 0.0f, 0.0f, 2.0f ), 10000, false );
            // gameRenderWorld.DebugLine( colorCyan, lastFrameVec, view.vieworg, 10000, false );
            // gameRenderWorld.DebugLine( colorYellow, view.vieworg + view.viewaxis[ 0 ] * 64.0f, view.vieworg + view.viewaxis[ 0 ] * 66.0f, 10000, false );
            // gameRenderWorld.DebugLine( colorOrange, view.vieworg + view.viewaxis[ 0 ] * 64.0f, view.vieworg + view.viewaxis[ 0 ] * 64.0f + idVec3( 0.0f, 0.0f, 2.0f ), 10000, false );
            // lastFrameVec = view.vieworg;
            // lastFrame = gameLocal.time;
            // }
// }
            if (g_showcamerainfo.GetBool()) {
                gameLocal.Printf("^5Frame: ^7%d/%d\n\n\n", realFrame + 1, this.camera.Num() - this.cameraCuts.Num());
            }
        }

        private void Start() {
            this.cycle = this.spawnArgs.GetInt("cycle");
            if (0 == this.cycle) {
                this.cycle = 1;
            }

            if (g_debugCinematic.GetBool()) {
                gameLocal.Printf("%d: '%s' start\n", gameLocal.framenum, GetName());
            }

            this.starttime = gameLocal.time;
            gameLocal.SetCamera(this);
            BecomeActive(TH_THINK);

            // if the player has already created the renderview for this frame, have him update it again so that the camera starts this frame
            if (gameLocal.GetLocalPlayer().GetRenderView().time == gameLocal.time) {
                gameLocal.GetLocalPlayer().CalculateRenderView();
            }
        }

        @Override
        public void Stop() {
            if (gameLocal.GetCamera() == this) {
                if (g_debugCinematic.GetBool()) {
                    gameLocal.Printf("%d: '%s' stop\n", gameLocal.framenum, GetName());
                }

                BecomeInactive(TH_THINK);
                gameLocal.SetCamera(null);
                if (this.threadNum != 0) {
                    idThread.ObjectMoveDone(this.threadNum, this);
                    this.threadNum = 0;
                }
                ActivateTargets(this.activator.GetEntity());
            }
        }

        @Override
        public void Think() {
            int frame;
            int frameTime;

            if ((this.thinkFlags & TH_THINK) != 0) {
                // check if we're done in the Think function when the cinematic is being skipped (idCameraAnim::GetViewParms isn't called when skipping cinematics).
                if (!gameLocal.skipCinematic) {
                    return;
                }

                if (this.camera.Num() < 2) {
                    // 1 frame anims never end
                    return;
                }

                if (this.frameRate == USERCMD_HZ) {
                    frameTime = gameLocal.time - this.starttime;
                    frame = frameTime / idGameLocal.msec;
                } else {
                    frameTime = (gameLocal.time - this.starttime) * this.frameRate;
                    frame = frameTime / 1000;
                }

                if (frame > ((this.camera.Num() + this.cameraCuts.Num()) - 2)) {
                    if (this.cycle > 0) {
                        this.cycle--;
                    }

                    if (this.cycle != 0) {
                        // advance start time so that we loop
                        this.starttime += ((this.camera.Num() - this.cameraCuts.Num()) * 1000) / this.frameRate;
                    } else {
                        Stop();
                    }
                }
            }
        }

        private void LoadAnim() {
            int version;
            final idLexer parser = new idLexer(LEXFL_ALLOWPATHNAMES | LEXFL_NOSTRINGESCAPECHARS | LEXFL_NOSTRINGCONCAT);
            final idToken token = new idToken();
            int numFrames;
            int numCuts;
            int i;
            idStr filename;
            final String key;

            key = this.spawnArgs.GetString("anim");
            if (null == key) {
                idGameLocal.Error("Missing 'anim' key on '%s'", this.name);
            }

            filename = new idStr(this.spawnArgs.GetString(va("anim %s", key)));
            if (0 == filename.Length()) {
                idGameLocal.Error("Missing 'anim %s' key on '%s'", key, this.name);
            }

            filename.SetFileExtension(MD5_CAMERA_EXT);
            if (!parser.LoadFile(filename)) {
                idGameLocal.Error("Unable to load '%s' on '%s'", filename, this.name);
            }

            this.cameraCuts.Clear();
            this.cameraCuts.SetGranularity(1);
            this.camera.Clear();
            this.camera.SetGranularity(1);

            parser.ExpectTokenString(MD5_VERSION_STRING);
            version = parser.ParseInt();
            if (version != MD5_VERSION) {
                parser.Error("Invalid version %d.  Should be version %d\n", version, MD5_VERSION);
            }

            // skip the commandline
            parser.ExpectTokenString("commandline");
            parser.ReadToken(token);

            // parse num frames
            parser.ExpectTokenString("numFrames");
            numFrames = parser.ParseInt();
            if (numFrames <= 0) {
                parser.Error("Invalid number of frames: %d", numFrames);
            }

            // parse framerate
            parser.ExpectTokenString("frameRate");
            this.frameRate = parser.ParseInt();
            if (this.frameRate <= 0) {
                parser.Error("Invalid framerate: %d", this.frameRate);
            }

            // parse num cuts
            parser.ExpectTokenString("numCuts");
            numCuts = parser.ParseInt();
            if ((numCuts < 0) || (numCuts > numFrames)) {
                parser.Error("Invalid number of camera cuts: %d", numCuts);
            }

            // parse the camera cuts
            parser.ExpectTokenString("cuts");
            parser.ExpectTokenString("{");
            this.cameraCuts.SetNum(numCuts);
            for (i = 0; i < numCuts; i++) {
                this.cameraCuts.oSet(i, parser.ParseInt());
                if ((this.cameraCuts.oGet(i) < 1) || (this.cameraCuts.oGet(i) >= numFrames)) {
                    parser.Error("Invalid camera cut");
                }
            }
            parser.ExpectTokenString("}");

            // parse the camera frames
            parser.ExpectTokenString("camera");
            parser.ExpectTokenString("{");
            this.camera.SetNum(numFrames);
            for (i = 0; i < numFrames; i++) {
                final cameraFrame_t cam = new cameraFrame_t();
                parser.Parse1DMatrix(3, cam.t);
                parser.Parse1DMatrix(3, cam.q);
                cam.fov = parser.ParseFloat();
                this.camera.oSet(i, cam);
            }
            parser.ExpectTokenString("}");

            /*if (false){
             if ( !gameLocal.GetLocalPlayer() ) {
             return;
             }

             idDebugGraph gGraph;
             idDebugGraph tGraph;
             idDebugGraph qGraph;
             idDebugGraph dtGraph;
             idDebugGraph dqGraph;
             gGraph.SetNumSamples( numFrames );
             tGraph.SetNumSamples( numFrames );
             qGraph.SetNumSamples( numFrames );
             dtGraph.SetNumSamples( numFrames );
             dqGraph.SetNumSamples( numFrames );

             gameLocal.Printf( "\n\ndelta vec:\n" );
             float diff_t, last_t, t;
             float diff_q, last_q, q;
             diff_t = last_t = 0.0f;
             diff_q = last_q = 0.0f;
             for( i = 1; i < numFrames; i++ ) {
             t = ( camera[ i ].t - camera[ i - 1 ].t ).Length();
             q = ( camera[ i ].q.ToQuat() - camera[ i - 1 ].q.ToQuat() ).Length();
             diff_t = t - last_t;
             diff_q = q - last_q;
             gGraph.AddValue( ( i % 10 ) == 0 );
             tGraph.AddValue( t );
             qGraph.AddValue( q );
             dtGraph.AddValue( diff_t );
             dqGraph.AddValue( diff_q );

             gameLocal.Printf( "%d: %.8f  :  %.8f,     %.8f  :  %.8f\n", i, t, diff_t, q, diff_q  );
             last_t = t;
             last_q = q;
             }

             gGraph.Draw( colorBlue, 300.0f );
             tGraph.Draw( colorOrange, 60.0f );
             dtGraph.Draw( colorYellow, 6000.0f );
             qGraph.Draw( colorGreen, 60.0f );
             dqGraph.Draw( colorCyan, 6000.0f );
             }*/
        }

        private void Event_Start() {
            Start();
        }

        private void Event_Stop() {
            Stop();
        }

        private void Event_SetCallback() {
            if ((gameLocal.GetCamera() == this) && (0 == this.threadNum)) {
                this.threadNum = idThread.CurrentThreadNum();
                idThread.ReturnInt(true);
            } else {
                idThread.ReturnInt(false);
            }
        }

        private void Event_Activate(idEventArg<idEntity> _activator) {
            this.activator.oSet(_activator.value);
            if ((this.thinkFlags & TH_THINK) != 0) {
                Stop();
            } else {
                Start();
            }
        }

        @Override
        public Class.idClass CreateInstance() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public java.lang.Class<?> /*idTypeInfo*/ GetType() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public eventCallback_t<?> getEventCallBack(idEventDef event) {
            return eventCallbacks.get(event);
        }

        public static Map<idEventDef, eventCallback_t> getEventCallBacks() {
            return eventCallbacks;
        }

    }
}
