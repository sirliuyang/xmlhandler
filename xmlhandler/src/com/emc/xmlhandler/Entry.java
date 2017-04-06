package com.emc.xmlhandler;

public class Entry {
    public String Node;
    public String KEY_NAME = "key";
    public String KEY_DESC;
    public String VALUE_NAME = "value";
    public String VALUE_DESC;
    public String MERGE_NAME = "merge";
    public String MERGE_DESC = null;


    public String getKEY_DESC() {
        return KEY_DESC;
    }

    public void setKEY_DESC(String kEY_DESC) {
        KEY_DESC = kEY_DESC;
    }

    public String getVALUE_DESC() {
        return VALUE_DESC;
    }

    public void setVALUE_DESC(String vALUE_DESC) {
        VALUE_DESC = vALUE_DESC;
    }

    public String getMERGE_DESC() {
        return MERGE_DESC;
    }

    public void setMERGE_DESC(String mERGE_DESC) {
        MERGE_DESC = mERGE_DESC;
    }
 
    public String getNode() {
        return Node;
    }

    public void setNode(String node) {
        Node = node;
    }

    @Override
    public String toString() {
        return "Node:" + Node + KEY_NAME + " : " + KEY_DESC + " | " + VALUE_NAME + " : " + VALUE_DESC + " | "
                + MERGE_NAME + " : " + MERGE_DESC;
    }
}
