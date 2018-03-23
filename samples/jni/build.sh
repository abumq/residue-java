g++ -std=c++11 -I$JAVA_HOME/include/ -I$JAVA_HOME/include/darwin/ -I$JAVA_HOME/include/linux/ -lresidue -DELPP_THREAD_SAFE -fPIC -shared -o libresidue-jni.so residue-jni.cpp
javac ResidueJNIExample.java Residue.java
