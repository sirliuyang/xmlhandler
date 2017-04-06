package com.emc.xmlhandler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public static String parseKey(String data) {
        return parse("key", data);
    }

    public static String parseValue(String data) {
        return parse("value", data);
    }

    public static String parseMerge(String data) {
        return parse("merge", data);
    }

    // It will get the value of the key
    public static String parse(String filter, String data) {
        // String patternComplete = "^<.*/>$";
        String pattern = filter + "\\s*" + "=\\s*" + "\"";
        // Pattern r = Pattern.compile(patternComplete);
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(data);
        if (m.find()) {
            String temp;
            temp = data.substring(m.end());
            int idx = temp.indexOf("\"");
            temp = temp.substring(0, idx);
            return temp;
        }
        return null;
    }

    public static String getNode(String line) {
        if (line.contains("node")) {
            String pattern = "name\\s*=\\s*\"";
            String temp;
            Pattern r = Pattern.compile(pattern);
            Matcher m = r.matcher(line);
            if (m.find()) {
                temp = line.substring(m.end());
                int idx = temp.indexOf("\"");
                temp = temp.substring(0, idx).trim();
                return temp;
            }
        }
        return null;
    }

    // Returns the replacement line
    public static String getNewLine(String line, String newvalue) {
        StringBuffer result = new StringBuffer();
        String pattern = "value\\s*=\\s*\"";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(line);
        if (m.find()) {
            result.append(line.substring(0, m.end()));
            result.append(newvalue + "\" />");
            return result.toString();
        } else {
            return null;
        }
    }

    //Validate the input, if can only be 0 or 1
    public static boolean isInputValid(String input, String type){
        if(input==null)
            return false;
        
        
        if(type.equals("0-1")){
            if((input.trim().equals("0")||input.trim().equals("1")))
                return true;
        }
        if(type.equals("YN")){
            if((input.trim().equalsIgnoreCase("yes")||input.trim().equalsIgnoreCase("no")))
                return true;
        }
        
        return false;
    }
    
}
