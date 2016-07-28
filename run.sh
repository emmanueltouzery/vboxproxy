mvn install
cp vboxproxyguest/logback.xml ~/win8-vm-sharedfolder/
cp vboxproxyguest/target/vboxproxyguest-1.0-SNAPSHOT.jar ~/win8-vm-sharedfolder/
java -cp vboxproxyhost/target/vboxproxyhost-1.0-SNAPSHOT.jar com.github.emmanueltouzery.vboxproxyhost.App $*
