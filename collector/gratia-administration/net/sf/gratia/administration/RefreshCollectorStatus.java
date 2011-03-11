package net.sf.gratia.administration;

import net.sf.gratia.services.JMSProxy;
import net.sf.gratia.util.Configuration;
import net.sf.gratia.util.Logging;

// Simple class that is implemented in the packaging
// the gratia-administration service and in the
// packaging of the gratia-reporting service.
// In gratia-administration, we can reach the collector
// via JMS and actually do the update.
// In gratia-reporting, we can not be sure to reach it 
// (the collector might be on a different node).
public class RefreshCollectorStatus
{
   static boolean ExecuteRefresh() 
   {
      try { 
         JMSProxy proxy = (JMSProxy)
         java.rmi.Naming.lookup(Configuration.getProperties().getProperty("service.rmi.rmilookup") +
                                Configuration.getProperties().getProperty("service.rmi.service"));
         proxy.refreshStatus();
         return true;
      } catch (Exception e) {
         Logging.warning("RefreshCollectorStatus: unable to properly connect to the Collector",e);
         return false;
      }
   }
}