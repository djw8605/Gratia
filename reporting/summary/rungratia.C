#include "gratia.C+"

void rungratia(const char *dir= 0,int debug = 0, int mode = 0) {
   gDebugLevel = debug;
   if (! (mode&1)) sharing(dir);
   if (! (mode&2)) {
      int xsize = 1424; // 1392;
      int ysize =  712;
      
      runOSG(dir, xsize, ysize);

      xsize = 650;
      ysize = 325;

      runOSG(dir, xsize, ysize);
      
      xsize = 258;
      ysize = 129;

      runOSG(dir, xsize, ysize);
   }
}
