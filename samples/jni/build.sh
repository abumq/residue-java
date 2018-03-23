g++ -std=c++11 -I$JAVA_HOME/include/ -I$JAVA_HOME/include/darwin/ -lresidue -shared -o libresidue-jni.dylib residue-jni.cpp
javac ResidueJNIExample.java Residue.java
