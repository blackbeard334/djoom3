package neo.framework;

import static neo.framework.Common.common;
import static neo.framework.DeclManager.DECL_LEXER_FLAGS;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_AUDIO;
import static neo.framework.DeclManager.declType_t.DECL_EMAIL;
import static neo.framework.DeclManager.declType_t.DECL_VIDEO;
import neo.framework.DeclManager.idDecl;
import neo.idlib.Lib.idException;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWBACKSLASHSTRINGCONCAT;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWMULTICHARLITERALS;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWPATHNAMES;
import static neo.idlib.Text.Lexer.LEXFL_NOFATALERRORS;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGCONCAT;
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
            idLexer src = new idLexer();
            idToken token = new idToken();

            src.LoadMemory(_text, textLength, GetFileName(), GetLineNum());
            src.SetFlags(LEXFL_NOSTRINGCONCAT | LEXFL_ALLOWPATHNAMES | LEXFL_ALLOWMULTICHARLITERALS | LEXFL_ALLOWBACKSLASHSTRINGCONCAT | LEXFL_NOFATALERRORS);
            src.SkipUntilString("{");

            text = new idStr("");
            // scan through, identifying each individual parameter
            while (true) {

                if (!src.ReadToken(token)) {
                    break;
                }

                if (token.equals("}")) {
                    break;
                }

                if (0 == token.Icmp("subject")) {
                    src.ReadToken(token);
                    subject = token;
                    continue;
                }

                if (0 == token.Icmp("to")) {
                    src.ReadToken(token);
                    to = token;
                    continue;
                }

                if (0 == token.Icmp("from")) {
                    src.ReadToken(token);
                    from = token;
                    continue;
                }

                if (0 == token.Icmp("date")) {
                    src.ReadToken(token);
                    date = token;
                    continue;
                }

                if (0 == token.Icmp("text")) {
                    src.ReadToken(token);
                    if (!token.equals("{")) {
                        src.Warning("Email decl '%s' had a parse error", GetName());
                        return false;
                    }
                    while (src.ReadToken(token) && !token.equals("}")) {
                        text.Append(token);
                    }
                    continue;
                }

                if (0 == token.Icmp("image")) {
                    src.ReadToken(token);
                    image = token;
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
            return from.toString();
        }

        public String GetBody() {
            return text.toString();
        }

        public String GetSubject() {
            return subject.toString();
        }

        public String GetDate() {
            return date.toString();
        }

        public String GetTo() {
            return to.toString();
        }

        public String GetImage() {
            return image.toString();
        }
    };

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
            idLexer src = new idLexer();
            idToken token = new idToken();

            src.LoadMemory(_text, textLength, GetFileName(), GetLineNum());
            src.SetFlags(LEXFL_NOSTRINGCONCAT | LEXFL_ALLOWPATHNAMES | LEXFL_ALLOWMULTICHARLITERALS | LEXFL_ALLOWBACKSLASHSTRINGCONCAT | LEXFL_NOFATALERRORS);
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
                    videoName = token;
                    continue;
                }

                if (0 == token.Icmp("preview")) {
                    src.ReadToken(token);
                    preview = token;
                    continue;
                }

                if (0 == token.Icmp("video")) {
                    src.ReadToken(token);
                    video = token;
                    declManager.FindMaterial(video);
                    continue;
                }

                if (0 == token.Icmp("info")) {
                    src.ReadToken(token);
                    info = token;
                    continue;
                }

                if (0 == token.Icmp("audio")) {
                    src.ReadToken(token);
                    audio = token;
                    declManager.FindSound(audio);
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
            return video.toString();
        }

        public String GetWave() {
            return audio.toString();
        }

        public String GetVideoName() {
            return videoName.toString();
        }

        public String GetInfo() {
            return info.toString();
        }

        public String GetPreview() {
            return preview.toString();
        }
    };

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
            idLexer src = new idLexer();
            idToken token = new idToken();

            src.LoadMemory(text, textLength, GetFileName(), GetLineNum());
            src.SetFlags(LEXFL_NOSTRINGCONCAT | LEXFL_ALLOWPATHNAMES | LEXFL_ALLOWMULTICHARLITERALS | LEXFL_ALLOWBACKSLASHSTRINGCONCAT | LEXFL_NOFATALERRORS);
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
                    audioName = token;
                    continue;
                }

                if (0 == token.Icmp("audio")) {
                    src.ReadToken(token);
                    audio = token;
                    declManager.FindSound(audio);
                    continue;
                }

                if (0 == token.Icmp("info")) {
                    src.ReadToken(token);
                    info = token;
                    continue;
                }

                if (0 == token.Icmp("preview")) {
                    src.ReadToken(token);
                    preview = token;
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
            return audioName.toString();
        }

        public String GetWave() {
            return audio.toString();
        }

        public String GetInfo() {
            return info.toString();
        }

        public String GetPreview() {
            return preview.toString();
        }
    };

    public static class idDeclPDA extends idDecl {

        private idStrList videos;
        private idStrList audios;
        private idStrList emails;
        private idStr     pdaName;
        private idStr     fullName;
        private idStr     icon;
        private idStr     id;
        private idStr     post;
        private idStr     title;
        private idStr     security;
        private int       originalEmails;
        private int       originalVideos;
        //
        //

        public idDeclPDA() {
            videos = new idStrList();
            audios = new idStrList();
            emails = new idStrList();
            pdaName = new idStr();
            fullName = new idStr();
            icon = new idStr();
            id = new idStr();
            post = new idStr();
            title = new idStr();
            security = new idStr();
            originalEmails = originalVideos = 0;
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
            idLexer src = new idLexer();
            idToken token = new idToken();

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
                    pdaName.oSet(token);
                    continue;
                }

                if (0 == token.Icmp("fullname")) {
                    src.ReadToken(token);
                    fullName.oSet(token);
                    continue;
                }

                if (0 == token.Icmp("icon")) {
                    src.ReadToken(token);
                    icon.oSet(token);
                    continue;
                }

                if (0 == token.Icmp("id")) {
                    src.ReadToken(token);
                    id.oSet(token);
                    continue;
                }

                if (0 == token.Icmp("post")) {
                    src.ReadToken(token);
                    post.oSet(token);
                    continue;
                }

                if (0 == token.Icmp("title")) {
                    src.ReadToken(token);
                    title.oSet(token);
                    continue;
                }

                if (0 == token.Icmp("security")) {
                    src.ReadToken(token);
                    security.oSet(token);
                    continue;
                }

                if (0 == token.Icmp("pda_email")) {
                    src.ReadToken(token);
                    emails.Append(token.toString());
                    declManager.FindType(DECL_EMAIL, token);
                    continue;
                }

                if (0 == token.Icmp("pda_audio")) {
                    src.ReadToken(token);
                    audios.Append(token.toString());
                    declManager.FindType(DECL_AUDIO, token);
                    continue;
                }

                if (0 == token.Icmp("pda_video")) {
                    src.ReadToken(token);
                    videos.Append(token.toString());
                    declManager.FindType(DECL_VIDEO, token);
                    continue;
                }

            }

            if (src.HadError()) {
                src.Warning("PDA decl '%s' had a parse error", GetName());
                return false;
            }

            originalVideos = videos.Num();
            originalEmails = emails.Num();
            return true;
        }

        @Override
        public void FreeData() {
            videos.Clear();
            audios.Clear();
            emails.Clear();
            originalEmails = 0;
            originalVideos = 0;
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

            if (unique && (videos.Find(name) != null)) {
                return;
            }
            if (declManager.FindType(DECL_VIDEO, _name, false) == null) {
                common.Printf("Video %s not found\n", name);
                return;
            }
            videos.Append(name);
        }

        public void AddAudio(final String _name, boolean unique /*= true*/) throws idException {
            final idStr name = new idStr(_name);

            if (unique && (audios.Find(name) != null)) {
                return;
            }
            if (declManager.FindType(DECL_AUDIO, _name, false) == null) {
                common.Printf("Audio log %s not found\n", name);
                return;
            }
            audios.Append(name);
        }

        public void AddEmail(final String _name, boolean unique /*= true*/) throws idException {
            final idStr name = new idStr(_name);

            if (unique && (emails.Find(name) != null)) {
                return;
            }
            if (declManager.FindType(DECL_EMAIL, _name, false) == null) {
                common.Printf("Email %s not found\n", name);
                return;
            }
            emails.Append(name);
        }

        public void AddEmail(final String _name) throws idException {
            AddEmail(_name, true);
        }

        public void RemoveAddedEmailsAndVideos() {
            int num = emails.Num();
            if (originalEmails < num) {
                while (num != 0 && num > originalEmails) {
                    emails.RemoveIndex(--num);
                }
            }
            num = videos.Num();
            if (originalVideos < num) {
                while (num != 0 && num > originalVideos) {
                    videos.RemoveIndex(--num);
                }
            }
        }

        public int GetNumVideos() {
            return videos.Num();
        }

        public int GetNumAudios() {
            return audios.Num();
        }

        public int GetNumEmails() {
            return emails.Num();
        }

        public idDeclVideo GetVideoByIndex(int index) throws idException {
            if (index >= 0 && index < videos.Num()) {
                return (idDeclVideo) (declManager.FindType(DECL_VIDEO, videos.oGet(index), false));
            }
            return null;
        }

        public idDeclAudio GetAudioByIndex(int index) throws idException {
            if (index >= 0 && index < audios.Num()) {
                return (idDeclAudio) declManager.FindType(DECL_AUDIO, audios.oGet(index), false);
            }
            return null;
        }

        public idDeclEmail GetEmailByIndex(int index) throws idException {
            if (index >= 0 && index < emails.Num()) {
                return (idDeclEmail) declManager.FindType(DECL_EMAIL, emails.oGet(index), false);
            }
            return null;
        }

        public void SetSecurity(final String sec) {
            security.oSet(sec);
        }

        public String GetPdaName() {
            return pdaName.toString();
        }

        public String GetSecurity() {
            return security.toString();
        }

        public String GetFullName() {
            return fullName.toString();
        }

        public String GetIcon() {
            return icon.toString();
        }

        public String GetPost() {
            return post.toString();
        }

        public String GetID() {
            return id.toString();
        }

        public String GetTitle() {
            return title.toString();
        }
    };
}
