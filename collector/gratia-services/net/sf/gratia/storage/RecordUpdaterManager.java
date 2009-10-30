package net.sf.gratia.storage;

import java.util.Iterator;

public class RecordUpdaterManager implements RecordUpdater
{

   java.util.List Updaters;

   public RecordUpdaterManager() 
   {
      // Create an Updater Managers and add the default Updaters:
      //     CheckEndTime
      
      Updaters = new java.util.LinkedList();

   }
   
   public RecordUpdater AddUpdater(RecordUpdater upd) 
   {
      Updaters.add(upd);
      return upd;
   }
   
   public RecordUpdater PrependUpdater(RecordUpdater upd) 
   {
      Updaters.add(0,upd);
      return upd;
   }
   
   public boolean Update(Record rec) throws UpdateException
   {
       for (Iterator i = Updaters.iterator(); i.hasNext(); ) {
          RecordUpdater el = (RecordUpdater)i.next();
          if (!el.Update(rec)) {
             return false;
          }
       }
       return true;
   }
}
