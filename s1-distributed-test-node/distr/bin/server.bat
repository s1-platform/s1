SET CURRENTDIR="%cd%"
java -classpath ..\conf -server -Xms2048M -Xms1024M -Dconf=%CURRENTDIR%\..\conf -Dport=9000 -jar ..\lib\s1-distributed-test-node.jar >> ..\log\log.txt 2>&1