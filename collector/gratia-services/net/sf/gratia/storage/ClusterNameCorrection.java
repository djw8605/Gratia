/**
 * 
 */
package net.sf.gratia.storage;

/**
 * @author pcanal
 *
 */
public class ClusterNameCorrection {
  private long corrid;
  private long clusterid;
  private String ClusterName;
  private int type;

  public int getType() {
    return type;
  }

  public void setType(int type) {
    this.type = type;
  }

  public ClusterNameCorrection()
  {
  }

  public String toString() {
    String output = "Cluster correction: " + "clusterid: " + clusterid + " Name: " + ClusterName +
    " Corrid: " + corrid;
    return output;
  }

  public long getCorrid() {
    return corrid;
  }

  public void setCorrid(long corrid) {
    this.corrid = corrid;
  }

  public long getClusterid() {
    return clusterid;
  }

  public void setClusterid(long clusterid) {
    this.clusterid = clusterid;
  }

  public String getClusterName() {
    return ClusterName;
  }

  public void setClusterName(String clusterName) {
    ClusterName = clusterName;
  }

}
