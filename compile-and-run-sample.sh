TYPE=$1
if [ "$TYPE" == "" ];then
	TYPE=Application
fi
sh sync.sh
javac -d bin -sourcepath src -cp lib/*:bin/Residue.jar simple-sample/src/com/muflihun/residue/$TYPE.java
java -cp bin:bin/Residue.jar:lib/* com.muflihun.residue.$TYPE
