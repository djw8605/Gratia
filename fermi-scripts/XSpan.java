import java.util.*;

public class XSpan
{
  public int from = -1;
  public int to = -1;
  public String replace = "";
  
  public XSpan()
  {
  }
  
  public XSpan(int from,int to)
  {
    this.from = from;
    this.to = to;
  }
  
  public XSpan(int from,int to,String replace)
  {
    this.from = from;
    this.to = to;
    this.replace = replace;
  }
  
}
