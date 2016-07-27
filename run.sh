/usr/bin/VBoxManage guestproperty delete bb74cf65-d9af-40a6-804b-44162d8795dd testkey
/usr/bin/VBoxManage guestproperty delete bb74cf65-d9af-40a6-804b-44162d8795dd testkey0
/usr/bin/VBoxManage guestproperty delete bb74cf65-d9af-40a6-804b-44162d8795dd testkey1
/usr/bin/VBoxManage guestproperty delete bb74cf65-d9af-40a6-804b-44162d8795dd testkey2
/usr/bin/VBoxManage guestproperty delete bb74cf65-d9af-40a6-804b-44162d8795dd testkey3
/usr/bin/VBoxManage guestproperty delete bb74cf65-d9af-40a6-804b-44162d8795dd testkey4
/usr/bin/VBoxManage guestproperty delete bb74cf65-d9af-40a6-804b-44162d8795dd testkey5
/usr/bin/VBoxManage guestproperty delete bb74cf65-d9af-40a6-804b-44162d8795dd testkey6
/usr/bin/VBoxManage guestproperty delete bb74cf65-d9af-40a6-804b-44162d8795dd testkey7
/usr/bin/VBoxManage guestproperty delete bb74cf65-d9af-40a6-804b-44162d8795dd testkey8
/usr/bin/VBoxManage guestproperty delete bb74cf65-d9af-40a6-804b-44162d8795dd testkey9
/usr/bin/VBoxManage guestproperty delete bb74cf65-d9af-40a6-804b-44162d8795dd testkey10
/usr/bin/VBoxManage guestproperty delete bb74cf65-d9af-40a6-804b-44162d8795dd testkey11
/usr/bin/VBoxManage guestproperty delete bb74cf65-d9af-40a6-804b-44162d8795dd testkey12
/usr/bin/VBoxManage guestproperty delete bb74cf65-d9af-40a6-804b-44162d8795dd testkey13
/usr/bin/VBoxManage guestproperty delete bb74cf65-d9af-40a6-804b-44162d8795dd testkey14
/usr/bin/VBoxManage guestproperty delete bb74cf65-d9af-40a6-804b-44162d8795dd testkey15
mvn install
cp vboxproxyguest/logback.xml ~/win8-vm-sharedfolder/
cp vboxproxyguest/target/vboxproxyguest-1.0-SNAPSHOT.jar ~/win8-vm-sharedfolder/
java -cp vboxproxyhost/target/vboxproxyhost-1.0-SNAPSHOT.jar com.github.emmanueltouzery.vboxproxyhost.App
