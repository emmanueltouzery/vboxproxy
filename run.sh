/usr/bin/VBoxManage guestproperty delete bb74cf65-d9af-40a6-804b-44162d8795dd testkey
mvn install
cp vboxproxyguest/target/vboxproxyguest-1.0-SNAPSHOT.jar ~/win8-vm-sharedfolder/
java -cp vboxproxyhost/target/vboxproxyhost-1.0-SNAPSHOT.jar com.github.emmanueltouzery.vboxproxyhost.App
