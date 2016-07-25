mvn -f vboxproxyhost/pom.xml package
/home/emmanuel/programs/jdk1.7.0_71/bin/javac Main.java
cp Main.class ~/win8-vm-sharedfolder/
java -cp vboxproxyhost/target/vboxproxyhost-1.0-SNAPSHOT.jar com.github.emmanueltouzery.vboxproxyhost.App
