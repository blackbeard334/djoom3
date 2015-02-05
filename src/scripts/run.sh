#!/bin/sh

exec 1>/dev/null
set STEAM_PATH="+set fs_basepath '~/.local/share/Steam/steamapps/common/doom 3' +set com_allowConsole 1 +set si_pure 0"

LATEST_JAR="$(ls *.jar -Art | tail -n 1)"

#echo $LATEST_JAR

java -Djava.library.path=natives -XX:UseSSE=4 -XX:-UseSSE42Intrinsics -server -Dorg.lwjgl.util.Debug=true -jar $LATEST_JAR $STEAM_PATH
