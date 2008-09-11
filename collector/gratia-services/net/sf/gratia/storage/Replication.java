package net.sf.gratia.storage;

import java.lang.Comparable;

public class Replication implements Comparable {
    private int replicationid;
    private int registered;
    private int running;
    private int security;
    private String openconnection;
    private String secureconnection;
    private int frequency;
    private int dbid;
    private int rowcount;
    private String probename;
    private String recordtable;
    private int bundleSize;

    public int compareTo(Object obj) {
        try {
            Replication cmp = (Replication) obj;

            int result = openconnection.compareTo(cmp.getopenconnection());
            if (result == 0) {
                result = probename.compareTo(cmp.getprobename());
            }
            return result;
        }
        catch (Exception e) {
            return -1;
        }
    }

    public Replication() {
        initialValues("");
    }

    public Replication(String recordTable) {
        initialValues(recordTable);
    }

    private void initialValues(String recordTable) {
        setopenconnection("");
        setsecureconnection("");
        setprobename("");
        setrecordtable(recordTable);
        setfrequency(1);
    }

    public int getreplicationid() {
        return replicationid;
    }

    public void setreplicationid(int value) {
        replicationid = value;
    }

    public int getregistered() {
        return registered;
    }

    public void setregistered(int value) {
        registered = value;
    }

    public int getrunning() {
        return running;
    }

    public void setrunning(int value) {
        running = value;
    }

    public int getsecurity() {
        return security;
    }

    public void setsecurity(int value) {
        security = value;
    }

    public String getopenconnection() {
        return openconnection;
    }

    public void setopenconnection(String value) {
        openconnection = value;
    }

    public String getsecureconnection() {
        return secureconnection;
    }

    public void setsecureconnection(String value) {
        secureconnection = value;
    }

    // Convenience method
    public String getDestination() {
        if (security == 0) {
            return openconnection;
        } else {
            return secureconnection;
        }
    }

    public void setdbid(int value) {
        dbid = value;
    }

    public int getdbid() {
        return dbid;
    }

    public void setfrequency(int value) {
        frequency = value;
    }

    public int getfrequency() {
        return frequency;
    }

    public void setrowcount(int value) {
        rowcount = value;
    }

    public int getrowcount() {
        return rowcount;
    }

    public String getprobename() {
        return probename;
    }

    public void setprobename(String value) {
        probename = value;
    }

    public String getrecordtable() {
        return recordtable;
    }

    public void setrecordtable(String value) {
        recordtable = value;
    }

    public int getbundleSize() {
        return bundleSize;
    }

    public void setbundleSize(int value) {
        bundleSize = value;
    }

}
