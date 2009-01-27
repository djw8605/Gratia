package net.sf.gratia.reporting;

public class MenuItem {
   
   private String _name;
   private String _link;
   private String _display;
   private Boolean _reportFrame;
   
   public MenuItem(String name, String link, String display, Boolean reportFrame)
   {
      _name = name;
      _link = link;
      _display = display;
      _reportFrame = reportFrame;
   }
   
   public String getName(){
      return _name;
   }
   
   public String getLink(){
      return _link;
   }
   
   public String getDisplay(){
      return _display;
   }
   
   public Boolean requestReportFrame() {
      return _reportFrame;
   }
}
