g++ -std=c++11 -I$JAVA_HOME/include/ -I$JAVA_HOME/include/darwin/ -I$JAVA_HOME/include/linux/ -lresidue -DELPP_THREAD_SAFE -fPIC -shared -o bin/libresidue-jni.so residue-jni.cpp
javac -d bin/ ResidueJNIExample.java com/muflihun/residue/Residue.java
jar cvf bin/Residue.jar bin/com* bin/lib*
