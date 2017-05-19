package com.emc.xmlhandler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class McPreferenceProcessor {

    static String libFile;
    static String bakFile;
    static String current;

    static String mergeResultFile;
    static String logFile;

    static final List<String> excludes = new ArrayList<String>();
    static String Node = "";

    static List<Entry> entries_lib = new ArrayList<Entry>();
    static List<Entry> entries_bak = new ArrayList<Entry>();
    static List<Entry> entries_current = new ArrayList<Entry>();

    static boolean tag;
    static boolean noUpdate;

    public static void main(String[] args) {
        libFile = args[0];
        bakFile = args[1];
        current = args[2];

        logFile = args[3];
        String option = args[4].trim();

        loadAll();

        if (option != null) {
            if (option.equals("report")) {
                report();
            }
            if (option.equals("update")) {
                mergeResultFile = args[5];
                manualUpdate();
            }
        }

        // update
        // update(outputFile, prepareContent(current));
        System.exit(0);
    }

    public static void report() {
        update(logFile, prepareReport(current));
        System.out.println("Please check the report at:" + logFile);
    }

    // Load all file to container
    private static void loadAll() {
        load(libFile, entries_lib);
        load(bakFile, entries_bak);
        load(current, entries_current);
        loadExcludes();
    }

    public static void loadAll(String lib, String bak, String cur, String log) {
        libFile = lib;
        bakFile = bak;
        current = cur;
        logFile = log;
        load(libFile, entries_lib);
        load(bakFile, entries_bak);
        load(current, entries_current);
        loadExcludes();
    }

    private static void manualUpdate() {
        Map<String, String> result = prepareContentWithInteract(current);
        update(mergeResultFile, result.get("mcserver"));
        update(logFile, result.get("report"));
        System.out.println("Preference mcserver.xml has been update.");
        System.out.println("More detail, please check the report at:" + logFile);
    }

    // the content of line which will write to file
    // Merge logic is here
    private static String finalValue(String line, String node) {

        String result = null;
        String key = Utils.parseKey(line);
        String bakValue = valueFromBak(key, node);
        String libValue = valueFromLib(key, node);;
        String libMergeType = mergeInfoFromLib(key, node);
        String bakMergeType = mergeInfoFromBak(key, node);

        // Excludes the special case
        if (!isExcluded(key)) {
            if (bakValue == null) {
                result = libValue;
            } else {
                if (libMergeType == null || libMergeType.equals("")) {
                    result = bakValue;
                } else {
                    if (libMergeType.equals("newvalue")) {
                        if (bakMergeType != null && bakMergeType.equals("keep"))
                            result = bakValue;
                        else
                            result = libValue;
                    }
                }
            }
        }
        return result;
    }

    private static String valueFromLib(String key, String node) {
        return findValueByKey(key, node, entries_lib);
    }

    private static String valueFromBak(String key, String node) {
        return findValueByKey(key, node, entries_bak);
    }

    // We think the merge of Lib must be in the same line, if merge is null, that only means there
    // is no merge
    private static String mergeInfoFromLib(String key, String node) {
        return findMergeByKey(key, node, entries_lib);
    }

    // TODO : if user wrap the line, the merge cannot be read
    private static String mergeInfoFromBak(String key, String node) {
        return findMergeByKey(key, node, entries_bak);
    }

    // You can do the backup
    private static void copy(String from, String to) {
        File source = new File(from);
        File target = new File(to);
        FileChannel in = null;
        FileChannel out = null;
        FileInputStream inStream = null;
        FileOutputStream outStream = null;
        try {
            inStream = new FileInputStream(source);
            outStream = new FileOutputStream(target);
            in = inStream.getChannel();
            out = outStream.getChannel();
            in.transferTo(0, in.size(), out);
        } catch (IOException e) {
            System.out.println("Cannot open I/O stream during copy");
            System.exit(1);
        } finally {
            try {
                inStream.close();
                in.close();
                outStream.close();
                out.close();
            } catch (IOException e) {
                System.out.println("Cannot close I/O stream after copy");
                System.exit(1);
            }

        }
    }

    private static void load(String path, List<Entry> entries) {
        BufferedReader br;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
            String data = null;
            Entry entry = null;
            while ((data = br.readLine()) != null) {
                setNode(data);
                if (/* parse("key", data) */ Utils.parseKey(data) != null) {
                    entry = convertToEntry(data, Node);
                    // System.out.println(entry);
                    entries.add(entry);
                }
            }

        } catch (FileNotFoundException e) {
            System.out.println("Cannot find file");
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Entry convertToEntry(String data, String node) {
        Entry entry = new Entry();
        /*
         * entry.setKEY_DESC(parse("key", data)); entry.setVALUE_DESC(parse("value", data));
         * entry.setMERGE_DESC(parse("merge", data));
         */
        entry.setKEY_DESC(Utils.parseKey(data));
        entry.setVALUE_DESC(Utils.parseValue(data));
        entry.setMERGE_DESC(Utils.parseMerge(data));
        entry.setNode(node);
        return entry;
    }

    private static String findValueByKey(String key, String node, List<Entry> entries) {
        for (Entry entry : entries) {
            if (entry.getKEY_DESC().equals(key) && entry.getNode().equals(node))
                return entry.getVALUE_DESC();
        }
        return null;
    }

    private static String findMergeByKey(String key, String node, List<Entry> entries) {
        for (Entry entry : entries) {
            if (entry.getKEY_DESC().equals(key) && entry.getNode().equals(node))
                return entry.getMERGE_DESC();
        }
        return null;
    }

    private static String prepareContent(String filePath) {
        BufferedReader br = null;
        String line = null;
        StringBuffer buf = new StringBuffer();

        try {
            br = new BufferedReader(new FileReader(filePath));
            while ((line = br.readLine()) != null) {
                setNode(line);
                // An Entry must have key and value, if not, ignore
                if (Utils.parseKey(line) != null && Node != null && Utils.parseValue(line) != null) {
                    if (mergeInfoFromLib(Utils.parseKey(line), Node) != null
                            && mergeInfoFromLib(Utils.parseKey(line), Node).equals("delete")) {
                        // System.out.println(line);
                        continue;
                    } else {
                        String replaceValue = finalValue(line, Node);
                        String newLine = Utils.getNewLine(line, replaceValue);
                        if (replaceValue != null && newLine != null) {
                            // Replace the value
                            buf.append(newLine + "\n");
                        } else {
                            buf.append(line + "\n");
                        }
                    }
                } else {
                    buf.append(line + "\n");
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Cannot find file");
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return buf.toString();
    }

    private static String prepareReport(String filePath) {
        BufferedReader br = null;
        String line = null;
        StringBuffer buf = new StringBuffer();

        try {
            br = new BufferedReader(new FileReader(filePath));
            noUpdate = true;
            while ((line = br.readLine()) != null) {
                setNode(line);
                // An Entry must have key and value, if not, ignore
                if (Utils.parseKey(line) != null && Node != null && Utils.parseValue(line) != null) {
                    if (mergeInfoFromLib(Utils.parseKey(line), Node) != null
                            && mergeInfoFromLib(Utils.parseKey(line), Node).equals("delete")) {
                        // System.out.println(line);
                        continue;
                    } else {
                        String replaceValue = finalValue(line, Node);
                        String newLine = Utils.getNewLine(line, replaceValue);
                        if (replaceValue != null && newLine != null) {
                            if (!replaceValue.equals(Utils.parseValue(line))) {
                                noUpdate = false;
                                // Replace the value
                                buf.append("Unexpected result at node : "+Node+"\nCurrent Entry:\n" + line + "\nRecommended Entry: \n"
                                        + newLine + "\n\n");
                            }

                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Cannot find file");
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        if (noUpdate) {
            System.out.println("There is no difference, exit.");
            return "Preference is correct.";
        }

        System.out.println("-----------------Difference Report-----------------");
        System.out.println(buf.toString());
        System.out.println("---------------------------------------------------");
        return buf.toString();
    }

    private static Map<String, String> prepareContentWithInteract(String filePath) {
        BufferedReader br = null;
        String line = null;
        Map<String, String> resultMap = new HashMap<String, String>();

        StringBuffer bufXml = new StringBuffer();
        StringBuffer bufReport = new StringBuffer();
        tag = false;
        noUpdate = true;
        try {
            br = new BufferedReader(new FileReader(filePath));
            while ((line = br.readLine()) != null) {
                setNode(line);
                String currentKey = Utils.parseKey(line);
                String currentValue = Utils.parseValue(line);
                // An Entry must have key and value, if not, ignore
                if (currentKey != null && Node != null && currentValue != null) {
                    if (mergeInfoFromLib(Utils.parseKey(line), Node) != null
                            && mergeInfoFromLib(Utils.parseKey(line), Node).equals("delete")) {
                        // System.out.println(line);
                        continue;
                    } else {
                        String replaceValue = finalValue(line, Node);
                        String newLine = Utils.getNewLine(line, replaceValue);
                        if (replaceValue != null && newLine != null && !replaceValue.equals(currentValue)) {
                            noUpdate = false;
                            if (tag == false) {
                                System.out.println(
                                        "Please input \"0\" or \"1\" to select each entry's value to update (\"0\" to select current value or \"1\" to select recommended value):");
                                bufReport.append("Summary Of Update Result:\n");
                                bufReport.append(
                                        "+---------------------------------------------------------------------------------+\n");
                                String reportHeader = String.format("|%-40s|%-40s|\n", "Key", "Value");
                                bufReport.append(reportHeader);
                                bufReport.append(
                                        "|---------------------------------------------------------------------------------|\n");
                                tag = true;
                            }
                            System.out.println(
                                    "+-------------------------------------------------------------------------------------------+");
                            String s = String.format("|%-36s|%-32s|%-21s|", "Key", "0-Current Value",
                                    "1-Recommended Value");
                            System.out.println(s);
                            System.out.println(
                                    "|-------------------------------------------------------------------------------------------|");
                            String s1 = String.format("|%-36s|%-32s|%-21s|", currentKey, currentValue, replaceValue);
                            System.out.println(s1);
                            System.out.println(
                                    "+-------------------------------------------------------------------------------------------+");
                            System.out.print("Your choice:");
                            BufferedReader intputReader = new BufferedReader(new InputStreamReader(System.in));
                            String inputText = intputReader.readLine();
                            while (!Utils.isInputValid(inputText, "0-1")) {
                                System.out.println("Incorrect input, please only input 0 or 1, then press \"Enter\"");
                                System.out.println("Your choice:");
                                intputReader = new BufferedReader(new InputStreamReader(System.in));
                                inputText = intputReader.readLine();
                            }
                            //
                            if (inputText.trim().equals("0")) {
                                bufXml.append(line + "\n");

                                String reportLine1 = String.format("|%-40s|%-40s|\n", currentKey, currentValue);
                                bufReport.append(reportLine1);
                                bufReport.append(
                                        "|---------------------------------------------------------------------------------|\n");

                            }
                            if (inputText.trim().equals("1")) {
                                bufXml.append(newLine + "\n");

                                String reportLine3 = String.format("|%-40s|%-40s|\n", currentKey, replaceValue);
                                bufReport.append(reportLine3);
                                bufReport.append(
                                        "|---------------------------------------------------------------------------------|\n");

                            }
                        } else {
                            bufXml.append(line + "\n");
                        }
                    }
                } else {
                    bufXml.append(line + "\n");
                }
            }
            tag = false;
        } catch (FileNotFoundException e) {
            System.out.println("Cannot find file");
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        if (noUpdate) {
            System.out.println("There is no difference, exit.");
            System.exit(55);
        }
        System.out.println(bufReport.toString());
        System.out.println("The preference mcserver.xml will be updated as above, are you sure ?");
        System.out.println("\"Yes\" or \"No\":");
        BufferedReader intputReader = new BufferedReader(new InputStreamReader(System.in));
        String inputText;
        try {
            inputText = intputReader.readLine();
            while (!Utils.isInputValid(inputText, "YN")) {
                System.out.println("Incorrect input, please input \"yes\" or \"no\", then press \"Enter\"");
                System.out.print("Your choice:");
                intputReader = new BufferedReader(new InputStreamReader(System.in));
                inputText = intputReader.readLine();
            }
            if (inputText.trim().equalsIgnoreCase("no")) {
                System.exit(55);
            }
        } catch (IOException e) {
            System.out.println("Cannot read input value");
            System.exit(1);
        }

        resultMap.put("mcserver", bufXml.toString());
        resultMap.put("report", bufReport.toString());
        return resultMap;
    }

    private static void update(String filePath, String content) {
        BufferedWriter bw = null;

        try {
            bw = new BufferedWriter(new FileWriter(filePath));
            bw.write(content);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    bw = null;
                }
            }
        }
    }

    private static boolean isExcluded(String keyname) {
        for (String key : excludes) {
            if (key.equals(keyname))
                return true;
        }
        return false;
    }

    private static void setNode(String line) {
        String node = Utils.getNode(line);
        if (node != null) {
            Node = node;
        }
    }

    public static void loadExcludes() {
        // version
        excludes.add("version");
        // Java heap
        excludes.add("maxJavaHeap");
        // password
        excludes.add("rmi_ssl_keystore_ap");
        excludes.add("backuprestoreAP");
        excludes.add("backuponlyAP");
        excludes.add("rootAP");
        excludes.add("MCUSERAP");
        excludes.add("restoreonlyAP");
        excludes.add("viewuserAP");
        // Security
        excludes.add("secure_agents_mode");
        excludes.add("secure_st_mode");
        excludes.add("encrypt_server_authenticate");
        excludes.add("secure_agent_feature_on");
        excludes.add("session_ticket_feature_on");
        excludes.add("secure_dd_feature_on");
        excludes.add("mc_gen_rsa_tls_valid_days");
    }

}
