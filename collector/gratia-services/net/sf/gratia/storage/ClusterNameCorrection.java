/**
 * 
 */
package net.sf.gratia.storage;

/**
 * @author pcanal
 *
 */
public class ClusterNameCorrection {
  private int corrid;
  private int clusterid;
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

  public int getCorrid() {
    return corrid;
  }

  public void setCorrid(int corrid) {
    this.corrid = corrid;
  }

  public int getClusterid() {
    return clusterid;
  }

  public void setClusterid(int clusterid) {
    this.clusterid = clusterid;
  }

  public String getClusterName() {
    return ClusterName;
  }

  public void setClusterName(String clusterName) {
    ClusterName = clusterName;
  }

}
