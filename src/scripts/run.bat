@ECHO OFF
SET STEAM_PATH="+set fs_basepath 'C:\Program Files (x86)\Steam\steamapps\common\doom 3' +set com_allowConsole 1 +set si_pure 0"

FOR /F "delims=|" %%I IN ('DIR "*.jar" /B /O:-D') DO (
	SET LATEST_JAR=%%I
	GOTO BREAK
)
:BREAK

REM @ECHO ON
REM ECHO %LATEST_JAR%

java -Djava.library.path=natives -XX:UseSSE=4 -XX:-UseSSE42Intrinsics -server -Dorg.lwjgl.util.Debug=true -jar %LATEST_JAR% %STEAM_PATH%
