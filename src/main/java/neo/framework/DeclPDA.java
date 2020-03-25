package neo.framework;

import static neo.framework.Common.common;
import static neo.framework.DeclManager.DECL_LEXER_FLAGS;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_AUDIO;
import static neo.framework.DeclManager.declType_t.DECL_EMAIL;
import static neo.framework.DeclManager.declType_t.DECL_VIDEO;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWBACKSLASHSTRINGCONCAT;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWMULTICHARLITERALS;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWPATHNAMES;
import static neo.idlib.Text.Lexer.LEXFL_NOFATALERRORS;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGCONCAT;

import neo.framework.DeclManager.idDecl;
import neo.idlib.Lib.idException;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.StrList.idStrList;

/**
 *
 */
public class DeclPDA {

    /*
     ===============================================================================

     idDeclPDA

     ===============================================================================
     */
    public static class idDeclEmail extends idDecl {

        private idStr text;
        private idStr subject;
        private idStr date;
        private idStr to;
        private idStr from;
        private idStr image;
        //
        //

        public idDeclEmail() {
        }

        @Override
        public long Size() {
//            return sizeof( idDeclEmail );
            return super.Size();
        }

        @Override
        public String DefaultDefinition() {
            {
                return "{\n"
                        + "\t" + "{\n"
                        + "\t\t" + "to\t5Mail recipient\n"
                        + "\t\t" + "subject\t5Nothing\n"
                        + "\t\t" + "from\t5No one\n"
                        + "\t" + "}\n"
                        + "}";
            }
        }

        @Override
        public boolean Parse(String _text, int textLength) throws idException {
            final idLexer src = new idLexer();

            src.LoadMemory(_text, textLength, GetFileName(), GetLineNum());
            src.SetFlags(LEXFL_NOSTRINGCONCAT | LEXFL_ALLOWPATHNAMES | LEXFL_ALLOWMULTICHARLITERALS | LEXFL_ALLOWBACKSLASHSTRINGCONCAT | LEXFL_NOFATALERRORS);
            src.SkipUntilString("{");

            this.text = new idStr("");
            // scan through, identifying each individual parameter
            while (true) {
                final idToken token = new idToken();

                if (!src.ReadToken(token)) {
                    break;
                }

                if (token.equals("}")) {
                    break;
                }

                if (0 == token.Icmp("subject")) {
                    src.ReadToken(token);
                    this.subject = token;
                    continue;
                }

                if (0 == token.Icmp("to")) {
                    src.ReadToken(token);
                    this.to = token;
                    continue;
                }

                if (0 == token.Icmp("from")) {
                    src.ReadToken(token);
                    this.from = token;
                    continue;
                }

                if (0 == token.Icmp("date")) {
                    src.ReadToken(token);
                    this.date = token;
                    continue;
                }

                if (0 == token.Icmp("text")) {
                    src.ReadToken(token);
                    if (!token.equals("{")) {
                        src.Warning("Email decl '%s' had a parse error", GetName());
                        return false;
                    }
                    while (src.ReadToken(token) && !token.equals("}")) {
                        this.text.Append(token);
                    }
                    continue;
                }

                if (0 == token.Icmp("image")) {
                    src.ReadToken(token);
                    this.image = token;
                    continue;
                }
            }

            if (src.HadError()) {
                src.Warning("Email decl '%s' had a parse error", GetName());
                return false;
            }
            return true;
        }

        @Override
        public void FreeData() {
        }

        @Override
        public void Print() throws idException {
            common.Printf("Implement me\n");
        }

        @Override
        public void List() throws idException {
            common.Printf("Implement me\n");
        }
//

        public String GetFrom() {
            return this.from.toString();
        }

        public String GetBody() {
            return this.text.toString();
        }

        public String GetSubject() {
            return this.subject.toString();
        }

        public String GetDate() {
            return this.date.toString();
        }

        public String GetTo() {
            return this.to.toString();
        }

        public String GetImage() {
            return this.image.toString();
        }
    }

    public static class idDeclVideo extends idDecl {

        private idStr preview;
        private idStr video;
        private idStr videoName;
        private idStr info;
        private idStr audio;//TODO:construction!?
        //
        //

        public idDeclVideo() {
        }

        @Override
        public long Size() {
//            return sizeof( idDeclEmail );
            return super.Size();
        }

        @Override
        public String DefaultDefinition() {
            return "{\n"
                    + "\t" + "{\n"
                    + "\t\t" + "name\t5Default Video\n"
                    + "\t" + "}\n"
                    + "}";
        }

        @Override
        public boolean Parse(String _text, int textLength) throws idException {
            final idLexer src = new idLexer();

            src.LoadMemory(_text, textLength, GetFileName(), GetLineNum());
            src.SetFlags(LEXFL_NOSTRINGCONCAT | LEXFL_ALLOWPATHNAMES | LEXFL_ALLOWMULTICHARLITERALS | LEXFL_ALLOWBACKSLASHSTRINGCONCAT | LEXFL_NOFATALERRORS);
            src.SkipUntilString("{");

            // scan through, identifying each individual parameter
            while (true) {
                final idToken token = new idToken();

                if (!src.ReadToken(token)) {
                    break;
                }

                if (token.equals("}")) {
                    break;
                }

                if (0 == token.Icmp("name")) {
                    src.ReadToken(token);
                    this.videoName = token;
                    continue;
                }

                if (0 == token.Icmp("preview")) {
                    src.ReadToken(token);
                    this.preview = token;
                    continue;
                }

                if (0 == token.Icmp("video")) {
                    src.ReadToken(token);
                    this.video = token;
                    declManager.FindMaterial(this.video);
                    continue;
                }

                if (0 == token.Icmp("info")) {
                    src.ReadToken(token);
                    this.info = token;
                    continue;
                }

                if (0 == token.Icmp("audio")) {
                    src.ReadToken(token);
                    this.audio = token;
                    declManager.FindSound(this.audio);
                    continue;
                }

            }

            if (src.HadError()) {
                src.Warning("Video decl '%s' had a parse error", GetName());
                return false;
            }
            return true;
        }

        @Override
        public void FreeData() {
        }

        @Override
        public void Print() throws idException {
            common.Printf("Implement me\n");
        }

        @Override
        public void List() throws idException {
            common.Printf("Implement me\n");
        }

        public String GetRoq() {
            return this.video.toString();
        }

        public String GetWave() {
            return this.audio.toString();
        }

        public String GetVideoName() {
            return this.videoName.toString();
        }

        public String GetInfo() {
            return this.info.toString();
        }

        public String GetPreview() {
            return this.preview.toString();
        }
    }

    public static class idDeclAudio extends idDecl {

        private idStr audio;
        private idStr audioName;
        private idStr info;
        private idStr preview;//TODO:construction!?
        //
        //

        public idDeclAudio() {
        }

        @Override
        public long Size() {
//            return sizeof( idDeclEmail );
            return super.Size();
        }

        @Override
        public String DefaultDefinition() {
            return "{\n"
                    + "\t" + "{\n"
                    + "\t\t" + "name\t5Default Audio\n"
                    + "\t" + "}\n"
                    + "}";
        }

        @Override
        public boolean Parse(String text, int textLength) throws idException {
            final idLexer src = new idLexer();

            src.LoadMemory(text, textLength, GetFileName(), GetLineNum());
            src.SetFlags(LEXFL_NOSTRINGCONCAT | LEXFL_ALLOWPATHNAMES | LEXFL_ALLOWMULTICHARLITERALS | LEXFL_ALLOWBACKSLASHSTRINGCONCAT | LEXFL_NOFATALERRORS);
            src.SkipUntilString("{");

            // scan through, identifying each individual parameter
            while (true) {
                final idToken token = new idToken();

                if (!src.ReadToken(token)) {
                    break;
                }

                if (token.equals("}")) {
                    break;
                }

                if (0 == token.Icmp("name")) {
                    src.ReadToken(token);
                    this.audioName = token;
                    continue;
                }

                if (0 == token.Icmp("audio")) {
                    src.ReadToken(token);
                    this.audio = token;
                    declManager.FindSound(this.audio);
                    continue;
                }

                if (0 == token.Icmp("info")) {
                    src.ReadToken(token);
                    this.info = token;
                    continue;
                }

                if (0 == token.Icmp("preview")) {
                    src.ReadToken(token);
                    this.preview = token;
                    continue;
                }

            }

            if (src.HadError()) {
                src.Warning("Audio decl '%s' had a parse error", GetName());
                return false;
            }
            return true;
        }

        @Override
        public void FreeData() {
        }

        @Override
        public void Print() throws idException {
            common.Printf("Implement me\n");
        }

        @Override
        public void List() throws idException {
            common.Printf("Implement me\n");
        }

        public String GetAudioName() {
            return this.audioName.toString();
        }

        public String GetWave() {
            return this.audio.toString();
        }

        public String GetInfo() {
            return this.info.toString();
        }

        public String GetPreview() {
            return this.preview.toString();
        }
    }

    public static class idDeclPDA extends idDecl {

        private final idStrList videos;
        private final idStrList audios;
        private final idStrList emails;
        private final idStr     pdaName;
        private final idStr     fullName;
        private final idStr     icon;
        private final idStr     id;
        private final idStr     post;
        private final idStr     title;
        private final idStr     security;
        private int       originalEmails;
        private int       originalVideos;
        //
        //

        public idDeclPDA() {
            this.videos = new idStrList();
            this.audios = new idStrList();
            this.emails = new idStrList();
            this.pdaName = new idStr();
            this.fullName = new idStr();
            this.icon = new idStr();
            this.id = new idStr();
            this.post = new idStr();
            this.title = new idStr();
            this.security = new idStr();
            this.originalEmails = this.originalVideos = 0;
        }

        @Override
        public long Size() {
//            return sizeof( idDeclEmail );
            return super.Size();
        }

        @Override
        public String DefaultDefinition() {
            return "{\n"
                    + "\t" + "name  \"default pda\"\n"
                    + "}";
        }

        @Override
        public boolean Parse(final String text, final int textLength) throws idException {
            final idLexer src = new idLexer();
            final idToken token = new idToken();

            src.LoadMemory(text, textLength, GetFileName(), GetLineNum());
            src.SetFlags(DECL_LEXER_FLAGS);
            src.SkipUntilString("{");

            // scan through, identifying each individual parameter
            while (true) {

                if (!src.ReadToken(token)) {
                    break;
                }

                if (token.equals("}")) {
                    break;
                }

                if (0 == token.Icmp("name")) {
                    src.ReadToken(token);
                    this.pdaName.oSet(token);
                    continue;
                }

                if (0 == token.Icmp("fullname")) {
                    src.ReadToken(token);
                    this.fullName.oSet(token);
                    continue;
                }

                if (0 == token.Icmp("icon")) {
                    src.ReadToken(token);
                    this.icon.oSet(token);
                    continue;
                }

                if (0 == token.Icmp("id")) {
                    src.ReadToken(token);
                    this.id.oSet(token);
                    continue;
                }

                if (0 == token.Icmp("post")) {
                    src.ReadToken(token);
                    this.post.oSet(token);
                    continue;
                }

                if (0 == token.Icmp("title")) {
                    src.ReadToken(token);
                    this.title.oSet(token);
                    continue;
                }

                if (0 == token.Icmp("security")) {
                    src.ReadToken(token);
                    this.security.oSet(token);
                    continue;
                }

                if (0 == token.Icmp("pda_email")) {
                    src.ReadToken(token);
                    this.emails.Append(token.toString());
                    declManager.FindType(DECL_EMAIL, token);
                    continue;
                }

                if (0 == token.Icmp("pda_audio")) {
                    src.ReadToken(token);
                    this.audios.Append(token.toString());
                    declManager.FindType(DECL_AUDIO, token);
                    continue;
                }

                if (0 == token.Icmp("pda_video")) {
                    src.ReadToken(token);
                    this.videos.Append(token.toString());
                    declManager.FindType(DECL_VIDEO, token);
                    continue;
                }

            }

            if (src.HadError()) {
                src.Warning("PDA decl '%s' had a parse error", GetName());
                return false;
            }

            this.originalVideos = this.videos.Num();
            this.originalEmails = this.emails.Num();
            return true;
        }

        @Override
        public void FreeData() {
            this.videos.Clear();
            this.audios.Clear();
            this.emails.Clear();
            this.originalEmails = 0;
            this.originalVideos = 0;
        }

        @Override
        public void Print() throws idException {
            common.Printf("Implement me\n");
        }

        @Override
        public void List() throws idException {
            common.Printf("Implement me\n");
        }

        public void AddVideo(final String _name, boolean unique /*= true*/) throws idException {
            final idStr name = new idStr(_name);

            if (unique && (this.videos.Find(name) != null)) {
                return;
            }
            if (declManager.FindType(DECL_VIDEO, _name, false) == null) {
                common.Printf("Video %s not found\n", name);
                return;
            }
            this.videos.Append(name);
        }

        public void AddAudio(final String _name, boolean unique /*= true*/) throws idException {
            final idStr name = new idStr(_name);

            if (unique && (this.audios.Find(name) != null)) {
                return;
            }
            if (declManager.FindType(DECL_AUDIO, _name, false) == null) {
                common.Printf("Audio log %s not found\n", name);
                return;
            }
            this.audios.Append(name);
        }

        public void AddEmail(final String _name, boolean unique /*= true*/) throws idException {
            final idStr name = new idStr(_name);

            if (unique && (this.emails.Find(name) != null)) {
                return;
            }
            if (declManager.FindType(DECL_EMAIL, _name, false) == null) {
                common.Printf("Email %s not found\n", name);
                return;
            }
            this.emails.Append(name);
        }

        public void AddEmail(final String _name) throws idException {
            AddEmail(_name, true);
        }

        public void RemoveAddedEmailsAndVideos() {
            int num = this.emails.Num();
            if (this.originalEmails < num) {
                while ((num != 0) && (num > this.originalEmails)) {
                    this.emails.RemoveIndex(--num);
                }
            }
            num = this.videos.Num();
            if (this.originalVideos < num) {
                while ((num != 0) && (num > this.originalVideos)) {
                    this.videos.RemoveIndex(--num);
                }
            }
        }

        public int GetNumVideos() {
            return this.videos.Num();
        }

        public int GetNumAudios() {
            return this.audios.Num();
        }

        public int GetNumEmails() {
            return this.emails.Num();
        }

        public idDeclVideo GetVideoByIndex(int index) throws idException {
            if ((index >= 0) && (index < this.videos.Num())) {
                return (idDeclVideo) (declManager.FindType(DECL_VIDEO, this.videos.oGet(index), false));
            }
            return null;
        }

        public idDeclAudio GetAudioByIndex(int index) throws idException {
            if ((index >= 0) && (index < this.audios.Num())) {
                return (idDeclAudio) declManager.FindType(DECL_AUDIO, this.audios.oGet(index), false);
            }
            return null;
        }

        public idDeclEmail GetEmailByIndex(int index) throws idException {
            if ((index >= 0) && (index < this.emails.Num())) {
                return (idDeclEmail) declManager.FindType(DECL_EMAIL, this.emails.oGet(index), false);
            }
            return null;
        }

        public void SetSecurity(final String sec) {
            this.security.oSet(sec);
        }

        public String GetPdaName() {
            return this.pdaName.toString();
        }

        public String GetSecurity() {
            return this.security.toString();
        }

        public String GetFullName() {
            return this.fullName.toString();
        }

        public String GetIcon() {
            return this.icon.toString();
        }

        public String GetPost() {
            return this.post.toString();
        }

        public String GetID() {
            return this.id.toString();
        }

        public String GetTitle() {
            return this.title.toString();
        }
    }
}
