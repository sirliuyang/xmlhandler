package com.emc.xmlhandler;

import java.util.ArrayList;
import java.util.List;

public class Test {
    static String rootPath = "C:\\Users\\liul41\\Desktop\\Work_Related\\ci\\";
    static String libFile = rootPath + "mcserver.xml";
    static String bakFile = rootPath + "mcserver.xml.b4upgrade";
    static String current = rootPath + "current.xml";
    // static String temp = rootPath + "temp.xml";
    static String logFile = rootPath + "report.log";
    static String mergeResultFile = rootPath + "mcserver.result.xml";


    public static void main(String[] args) {
        McPreferenceProcessor.loadAll(libFile, bakFile, current, logFile);

        McPreferenceProcessor.report();
    }
}
