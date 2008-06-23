package net.sf.gratia.storage;

import java.sql.*;

import org.hibernate.Session;

public class SummaryUpdater {

    SummaryUpdater() { }

    public static void removeFromSummary(int dbid, Session session)
        throws Exception {

        Statement statement = session.connection().createStatement();
        statement
            .execute("call del_JUR_from_summary(" + dbid + ")");
    }

    public static void addToSummary(int dbid, Session session)
        throws Exception {
        
        Statement statement = session.connection().createStatement();
        statement
            .execute("call add_JUR_to_summary(" + dbid + ")");
    }

}
