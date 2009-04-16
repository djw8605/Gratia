package net.sf.gratia.storage;

/**
 * <p>Title: AttachableCollection</p>
 *
 * <p>Description: Interface to allow caching of 'sub' elements that persist beyhond a single record.</p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: </p>
 *
 * @author Philippe Canal
 * @version 1.0
 */

import net.sf.gratia.util.Logging;

public class AttachableCollection<Type extends AttachableXmlElement> {

   private java.util.Map<String, Type> fSaved = new java.util.HashMap<String,Type>();
   private long fMaxRecord = -1;
   private boolean fEnable;
   
   public AttachableCollection() 
   {
      
   }
   
   public synchronized void setCaching(boolean enable) {
      if (enable) {
         fEnable = true;
      } else {
         fEnable = false;
         fSaved.clear();
      }
   }
   
   public synchronized Type getObject( Type check ) throws Exception
   {
      return fSaved.get( check.getmd5() );
   }
   
   public synchronized void setObject( Type obj ) throws Exception
   {
      if (fEnable) {
         if (fMaxRecord == -1) {
            fMaxRecord = 1000;
            java.util.Properties p = net.sf.gratia.util.Configuration.getProperties();         
            String cname = obj.getClass().getName();
            cname = cname.substring( cname.lastIndexOf('.')+1 );
            String value = p.getProperty("service.cachesize."+cname);
            if (value != null ) {
               try {
                  fMaxRecord = Long.parseLong(value);
                  Logging.fine("AttachableCollection for " + cname + " found cachesize of " + fMaxRecord);
               } catch (Exception e) {
                  Logging.warning("AttachableCollection: found problem with cachesize property " + value + 
                                  ": error was, " + e.getMessage() + " -- property ignored (using " + fMaxRecord + ")");
               }
            } else {
               Logging.fine("AttachableCollection for " + cname + " use default cachesize of " + fMaxRecord);            
            }
         }
         if (fSaved.size() > fMaxRecord) {
            // Need to remove some elements, since there is not (yet?) a good way to clear just a few
            // of the oldest record, let's remove them all ... this is not ideal 
            fSaved.clear();
         }
         fSaved.put( obj.getmd5(), obj );
      }
   }
   
   public Type getObject( org.hibernate.Session session, Type check )  throws Exception
   {
      Type obj = null;
      String command = "from " + check.getClass().getName() + " where md5 = ?";

      java.util.List result = session.createQuery(command).setString(0,check.getmd5()).list();
      
      if (result.size() == 0) {
         
         return null;
         
      } else if (result.size() >= 1) {
         
         for(int i=0; i<result.size(); ++i) {
            obj = (Type) result.get(i);

            // Should we cross check the md5?
            if ( check.asXml("").equals( obj.asXml("") ) ) {
               return obj;
            }
         }
      }
      return null;
      
   }
   
   public Type attach( org.hibernate.Session session, Type obj ) throws Exception
   {
      if (obj.getId() != 0) {
         // Logging.warning("connection for update: "+obj.toString());
         session.update( obj );
         return obj;
      }
      
      // First check whether obj is already in memory
      Type attached = getObject( obj );
      
      if (attached != null) 
      {
         // Logging.warning("connection from collection: "+attached.toString());
         session.update( attached );
         return attached;

      } else {
         
         // Check whether this Type is already in the db
         attached = getObject( session, obj );
         if (attached != null) 
         {
            // Logging.warning("connection from db: "+attached.toString());
            setObject( attached );
            return attached;
            
         } else {
            
            // Otherwise save it.
            session.save(obj);            
            // Logging.warning("connection from create: "+obj.toString());
            setObject( obj );
            return obj;
            
         }

      }
   }
}