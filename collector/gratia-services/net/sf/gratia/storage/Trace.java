package net.sf.gratia.storage;

import java.util.*;

public class Trace {
    private int traceId;
    private Date eventtime;
    private String procName;
    private String userKey;
    private String userName;
    private String userRole;
    private String userVO;
    private String sqlQuery;
    private String procTime;
    private String queryTime;
    private String p1;
    private String p2;
    private String p3;

    public int gettraceId() {
        return traceId;
    }

    public void settraceId(int traceId) {
        this.traceId = traceId;
    }

    public Date geteventtime() {
        return eventtime;
    }

    public void seteventtime(Date eventtime) {
        this.eventtime = eventtime;
    }

    public String getprocName() {
        return procName;
    }

    public void setprocName(String procName) {
        this.procName = procName;
    }

    public String getuserKey() {
        return userKey;
    }

    public void setuserKey(String userKey) {
        this.userKey = userKey;
    }

    public String getuserName() {
        return userName;
    }

    public void setuserName(String userName) {
        this.userName = userName;
    }

    public String getuserRole() {
        return userRole;
    }

    public void setuserRole(String userRole) {
        this.userRole = userRole;
    }

    public String getuserVO() {
        return userVO;
    }

    public void setuserVO(String userVO) {
        this.userVO = userVO;
    }

    public String getsqlQuery() {
        return sqlQuery;
    }

    public void setsqlQuery(String sqlQuery) {
        this.sqlQuery = sqlQuery;
    }

    public String getprocTime() {
        return procTime;
    }

    public void setprocTime(String procTime) {
        this.procTime = procTime;
    }

    public String getqueryTime() {
        return queryTime;
    }

    public void setqueryTime(String queryTime) {
        this.queryTime = queryTime;
    }

    public String getp1() {
        return p1;
    }

    public void setp1(String p1) {
        this.p1 = p1;
    }

    public String getp2() {
        return p2;
    }

    public void setp2(String p2) {
        this.p2 = p2;
    }

    public String getp3() {
        return p3;
    }

    public void setp3(String p3) {
        this.p3 = p3;
    }


}
