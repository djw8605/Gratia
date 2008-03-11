/*
 * StorageManager.java
 * Dec 14, 2005
 */
package net.sf.gratia.storage;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
// This is only in the newest hibernate
// import org.hibernate.tool.hbm2ddl.SchemaValidator;

/**
 * <p>Title: StorageManager</p>
 *
 * <p>Description: Initiliaze and maintain the hibernate connections and schema.</p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: SLAC</p>
 *
 * @author Matteo Melani
 * @version 1.0
 */

public class StorageManager
{
    public StorageManager()
    { 
    }

    public void init()
    {
        mLogger.info("StorageManager is initializing...");
        Configuration cfg = new Configuration();
        cfg.configure();
		// SchemaValidator validate = new SchemaValidator(cfg);
		mSessionFactory = cfg.buildSessionFactory();
		try
		{
			// validate.validate();
            mLogger.debug("StorageManager is attempting to update the database schema"); 
			SchemaUpdate update = new SchemaUpdate(cfg);
			update.execute(true, true);
		}
		catch (org.hibernate.HibernateException e)
		{
			mLogger.debug("Storage Manager update error was: " + e);
			// The udpate failed.
            mLogger.debug("StorageManager is attempting to create the database schema"); 
            SchemaExport schemaExport = new SchemaExport(cfg);
			schemaExport.create(true, true);
		}
        mLogger.debug("StorageManager is initialized"); 
    }
    
    public Session getSession()
    {   
        return mSessionFactory.openSession();
    }
       
    private Log mLogger = LogFactory.getLog(StorageManager.class);
    private SessionFactory mSessionFactory;
}
