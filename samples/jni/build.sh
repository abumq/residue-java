mkdir -p bin/lib/
g++ -std=c++11 -I$JAVA_HOME/include/ -I$JAVA_HOME/include/darwin/ -I$JAVA_HOME/include/linux/ -lresidue -DELPP_THREAD_SAFE -fPIC -shared -o bin/lib/libresidue-jni.so src/residue-jni.cc
javac -d bin/ src/cz/adamh/NativeUtils.java src/com/muflihun/residue/Residue.java src/com/muflihun/residue/Logger.java src/com/muflihun/residue/ResiduePrintStream.java
cd bin/
jar cvf Residue.jar cz* com* lib/lib*
cd ..
javac -d bin/ -cp bin/ app/ResidueJNIExample.java
# rm -rf bin/com bin/cz bin/lib
