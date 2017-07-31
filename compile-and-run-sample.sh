javac -d bin -sourcepath src -cp lib/*:bin/Residue.jar simple-sample/src/com/muflihun/Application.java
java -cp bin:bin/Residue.jar:lib/* com.muflihun.Application
