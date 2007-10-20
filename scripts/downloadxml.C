#ifndef __CINT__
#include <TSQLServer.h>
#include <TSQLResult.h>
#include <TSQLRow.h>
#include <TSQLStatement.h>
#include <TStopwatch.h>
#include <time.h>
#include <TDatime.h>
#include <TError.h>
#include <vector>
#include "TGraph.h"
#include "Riostream.h"
#include "TPad.h"
#include "TCanvas.h"
#include "TAxis.h"
#include <map>
#include "TSystem.h"
#include "TH1.h"
#include "THStack.h"
#endif

const int maxfiles = 10000;

void genericoutput(TSQLServer *db, const char*sql) {

   Info("Report","Running : %s\n",sql);
   TSQLStatement *stmt = db->Statement(sql);

   if (stmt==0) return;

   stmt->Process();
   stmt->StoreResult();
      
   int nfields = stmt->GetNumFields();
   for (int i = 0; i < nfields; i++) {
      printf("%40s", stmt->GetFieldName(i));
   }
   printf("\n");
   for (int i = 0; i < nfields*40; i++) {
      printf("=");
   }
   printf("\n");
   while (stmt->NextResultRow()) {
     
      for (int j = 0; j < nfields; j++) {
         printf("%40s (%lf)", stmt->GetString(j),stmt->GetDouble(j));
      }
      printf("\n");
   }     
   delete stmt;
}

void write(Int_t dbid, Int_t year, Int_t month, Int_t day, Int_t hour,const TString &what)
{
   static int count = 0;
   static int stride = 10000;

   int sub = dbid / maxfiles;

   TString where; where.Form("output/%d/%02d/%02d/%d-%d",year,month,day,hour,sub);
   gSystem->mkdir(where,kTRUE);
   
   if ( (count % stride) == 0) fprintf(stderr,"Will create %s\n",Form("%s/%d.xml",where.Data(),dbid));

   FILE *f = fopen(Form("%s/%d.xml",where.Data(),dbid),"w");
   if (f==0) {
      TString name; name.Form("%s/%d.xml",where.Data(),dbid);
      Error("write","Could not write the file for %s\n",name.Data());
      return;
   }
   fprintf(f,"%s",what.Data());
   fclose(f);

   ++count;
}

void process(TSQLServer *db, const char*sql) {

   Info("Report","Running : %s\n",sql);
   TSQLStatement *stmt = db->Statement(sql);

   if (stmt==0) return;

   stmt->Process();
   stmt->StoreResult();
      
   int row = 0;
   TString rawxml;
   while (stmt->NextResultRow()) {
      
      Int_t year, month, day, hour, min, sec, frac;
      if (!stmt->GetTimestamp(0, year, month, day, hour, min, sec, frac)) {
         Error("process","Failed to parse date for row #%d: %s\n",row,stmt->GetString(0));
      }
      rawxml = stmt->GetString(2);
      int dbid = stmt->GetInt(3);

      write(dbid,year,month,day,hour,rawxml);

   }     
   delete stmt;
}

TSQLServer *getServer() {
   static TSQLServer *db = TSQLServer::Connect("mysql://gratia-db01.fnal.gov:3320/gratia","reader", "reader");

   if (db==0) {
      Error("gratia","db not found");
      return 0;
   }
   printf("Server info: %s\n", db->ServerInfo());

   return db;
}


void downloadxml(TSQLServer *db, const char *to) {
   TString sql = Form("select ServerDate,ExtraXML,RawXML,X.dbid from JobUsageRecord_Meta M, JobUsageRecord_Xml X where M.dbid = X.dbid and %s;",to);
   
   process(db,sql);

}

void downloadxml(const char *to) {
   TSQLServer *db = getServer();
   downloadxml(db,to);
}

void downloadxml(TSQLServer *db, const char *from, const char *to, Long_t idfrom, Long_t idto) {
  TString where = Form(" '%s' <= ServerDate and ServerDate < '%s' and %d <= M.dbid and M.dbid < %d\n", from, to, idfrom, idto);

  downloadxml(db,where);
}

void downloadxml(const char *from, const char *to) {
  TSQLServer *db = getServer();

  Long_t stride =  1000000;
  for(Long_t l = 0; l < 26000000; l += stride) {
    downloadxml(db,from,to,l,l+stride);
  }
}
