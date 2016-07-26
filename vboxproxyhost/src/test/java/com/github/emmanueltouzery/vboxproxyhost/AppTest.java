package com.github.emmanueltouzery.vboxproxyhost;

import org.junit.*;

import javaslang.*;

public class AppTest
{
    @Test
    public void parseEnumeratePropFull()
    {
        Tuple2<String, String> r = App.parseEnumerateProp("Name: /VirtualBox/GuestInfo/OS/Product, value: Windows 7, timestamp: 1469513214689375000, flags:");
        Assert.assertEquals("/VirtualBox/GuestInfo/OS/Product", r._1);
        Assert.assertEquals("Windows 7", r._2);
    }

    @Test
    public void parseEnumeratePropNoValue()
    {
        Tuple2<String, String> r = App.parseEnumerateProp("Name: /VirtualBox/GuestInfo/OS/Product, value: , timestamp: 1469513214689375000, flags:");
        Assert.assertEquals("/VirtualBox/GuestInfo/OS/Product", r._1);
        Assert.assertEquals("", r._2);
    }
}
