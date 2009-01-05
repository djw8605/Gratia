#include <TROOT.h>
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
#include "TColor.h"
#include "TGaxis.h"
#include "TXMLAttr.h"
#include "TDOMParser.h"
#include "TXMLNode.h"
#include "TXMLDocument.h"

#include "siteOwner.h"

#include <algorithm>

int gDebugLevel = 0;

void genericoutput(TSQLServer *db, const char*sql);
double getOneDouble(TSQLServer *db, const char *query, int field = 0);
long getOneLong(TSQLServer *db, const char *query, int field = 0);

class OInfo {
public:
   OInfo() : fPercent(0), fNjobs(0), fWall(0), fGuessed(false) {}
   std::string fVO;
   short fPercent;
   long fNjobs;
   long fWall;
   bool fGuessed;
};

void sharing(FILE *out, FILE *outcsv, TSQLServer *db, TDatime *begin, TDatime *end) 
{
   char *buffer = new char[128];
   time_t value = begin->Convert();
   strftime(buffer,127, "%Y-%m-%d",localtime(&value));

   fprintf(out,"OSG usage summary (midnight to midnight UTC) for %s\n"
"including all jobs that finished in that time period.\n\n"
"The ownership information was extracted from OIM from http://myosg.grid.iu.edu/trunk/vo/?group=resource.\n"
"with some straightforward changes applied (for example ATLAS -> USATLAS),\n"
"Some of the information as not yet been updated in OIM and some attempt was\n"
"made to 'guess' the owner from previous information source; those guessed\n"
"owners appear in the table with the tag '(not in OIM)'.\n"
"OIM also sometimes use a generic name 'Other' for one of the owners; this can\n"
"not be associated with any information from Gratia.\n"
"The subgroups in the Fermilab VO are __not__ considered owners of the sites\n"
"operated by Fermilab.\n\n",buffer);

   TString todaystring = Form("For all jobs finished on %s (UTC)\n",buffer);
   delete [] buffer;


   const char *sql = "SELECT SiteName, Sum(Njobs), Sum(WallDuration), Sum(CpuUserDuration+CpuSystemDuration) "
   " FROM VOProbeSummary V, Probe, Site where V.ProbeName = Probe.ProbeName and Site.siteid = Probe.siteid and '%s' <= EndTime and EndTime < '%s' "
   " and VOName != 'unknown' and VOName != 'other' and ResourceType = 'Batch'  "
   " group by SiteName order by SiteName";
      
   TString sbegin( begin->AsSQLString() );
   TString send( end->AsSQLString() );
   TString query = Form(sql,sbegin.Data(),send.Data());

   if (gDebugLevel) Info("Report","Running : %s\n",query.Data());
   TSQLStatement *stmt = db->Statement(query.Data());

   if (stmt==0) return;

   stmt->Process();
   stmt->StoreResult();
      
   map<string,string> owners;
   TString cmd = "wget -q -O - http://oim.grid.iu.edu/pub/resource/show.php?format=plain-text | cut -d, -f1,4,7,8,14 | grep -e ',OSG,[^,]*,CE [^,]*,1' | cut -d, -f1,3";
   FILE * f = gSystem->OpenPipe(cmd,"r");
   //if (!f) {
      for(UInt_t i=0; i < sizeof(siteOwners)/sizeof(void*); i += 2) {
         std::string own( siteOwners[i+1] );
         std::transform(own.begin(), own.end(), own.begin(), ::tolower);
         owners[ siteOwners[i] ] = own;
      }
   //} else {
      char x;
      string name;
      string owner;
      bool left = true;
      string result;
      while ((x = fgetc(f))!=EOF ) {
         switch (x) {
             case ',': left = false; break;
             case '\n':
             case '\r':
                if (owner == "USCMS") {
                   owner = "CMS";
                }
                if (name == "SPRACE-CE") {
                   name = "SPRACE";
                }
                std::transform(owner.begin(), owner.end(), owner.begin(), ::tolower);
                owners[ name ] = owner;
                name = "";
                owner = "";
                left = true;
                break;
             default:
                if (left) {
                   name += x;
                } else {
                   owner += x;
                }
         }
      }
      fclose(f);
   //}
   
   typedef map<string, OInfo> InnerMap_t;
   map<string, InnerMap_t > ownerShare;
   cmd = "wget -q -O - http://myosg.grid.iu.edu/trunk/vo/xml?group=resource";
   f = gSystem->OpenPipe(cmd,"r");
   TString xmldoc;
   while ((x = fgetc(f))!=EOF ) {
      xmldoc.Append(x);
   }
   fclose(f);
   TDOMParser loader;
   loader.SetValidate(false);
   if ( 0 !=  loader.ParseBuffer( xmldoc.Data(), xmldoc.Length() ) ) {
      Error("Report","Could not properly parse the result of %s",cmd.Data());
      return;
   }
   TXMLNode *node = loader.GetXMLDocument()->GetRootNode();
   if (0 != strcmp(node->GetNodeName(),"ResourceOwnerships") ) {
      Error("Report","Node is not ResourceOwnerships as expected but %s",node->GetNodeName());
      return;
   }
   node = node->GetChildren();
   while (node) {
      if ( 0 != strcmp(node->GetNodeName(),"Resource") ) {
         Error("Report","Node is not Resource as expected but %s",node->GetNodeName());
         node = node->GetNextNode();
         continue;
      }
      {
         TXMLNode *sub = node->GetChildren();
         //fprintf(stderr,"Sub: %s\n",sub->GetNodeName());
         InnerMap_t owner_percents;
         std::string owner_name;
         std::string resource_name;
         int percent;
         while (sub) {
            if ( 0 == strcmp( "ResourceID", sub->GetNodeName() ) ) {
               // skip
            } else if ( 0 == strcmp ("ResourceName", sub->GetNodeName() ) ) {
               resource_name = sub->GetText();
            } else if ( 0 == strcmp ( "Ownership", sub->GetNodeName() ) ) {
               TXMLNode *nowner = sub->GetChildren();
               while( nowner ) {
                  if ( 0 != strcmp( "Percentage", nowner->GetNodeName() ) ) {
                     Error("Report","Node is not Percentage as expected but %s",node->GetNodeName());
                  }
                  if (nowner->GetAttributes()) {
                     TXMLAttr *attr = (TXMLAttr*)nowner->GetAttributes()->FindObject("VO");
                     if (attr) {
                        owner_name = attr->GetValue();
                        std::transform(owner_name.begin(), owner_name.end(), owner_name.begin(), ::tolower);
                     
                        if (owner_name == "atlas") {
                           owner_name = "usatlas";
                        }
                        if (owner_name == "other") {
                           owner_name = "Listed as other in OIM";
                        }
                        percent = atoi(nowner->GetText());
                        //fprintf(stderr,"text %s vs value %d\n",nowner->GetText(),percent);
                        owner_percents[ owner_name ].fVO = owner_name;
                        owner_percents[ owner_name ].fPercent = percent;
                     } else {
                        Error("ParsingXml","Percentage node does not specify a vo: %s\n",nowner->GetContent());
                     }
                  }
                  nowner = nowner->GetNextNode();
               }
            }
            sub = sub->GetNextNode();
         }
         ownerShare[ resource_name ] = owner_percents;
         //fprintf(stderr,"for %s %d\n",resource_name.c_str(), owner_percents.size());
      }
      node = node->GetNextNode();
   }
   

   TString pattern = 
      Form("SELECT VOName, Sum(Njobs),Sum(WallDuration), Sum(CpuUserDuration+CpuSystemDuration), SiteName  FROM VOProbeSummary V, Probe, Site where V.ProbeName = Probe.ProbeName and Site.siteid = Probe.siteid and '%s' < EndTime and EndTime < '%s' and SiteName = '%%s' and (UCASE(VOName) = '%%s') group by SiteName",sbegin.Data(),send.Data());

   TString pattern2 = 
     Form("SELECT LCASE(VOName), Sum(Njobs),Sum(WallDuration), Sum(CpuUserDuration+CpuSystemDuration), SiteName "
          "FROM VOProbeSummary V, Probe, Site where V.ProbeName = Probe.ProbeName and Site.siteid = Probe.siteid and '%s' <= EndTime and EndTime < '%s' and SiteName = '%%s' "
          "and VOName != 'unknown' and VOName != 'other' and ResourceType = 'Batch'  "
          "group by SiteName, LCASE(VOName)",sbegin.Data(),send.Data());

   long total_njobs = 0;
   long total_owner_njobs = 0;
   long total_wall = 0;
   long total_owner_wall = 0;
   // int nfields = stmt->GetNumFields();

   const char *dashes = "-----------------------------------";
   TString  dashFormat( "%6.6s-%22.22s-%8.8s-%31.31s-%11.11s-%9.9s-%11.11s-%9.9s\n");
   TString  textFormat( " %4s | %-20s | %7s | %-29s | %9s | %6s | %9s | %6s\n");
   TString valueFormat( " %2d.%1d | %-20s | %7ld | %-22s (%3.0d%%) | %9ld | %5.1f%% | %9ld | %5.1f%%\n");

   TString textFormat_csv("%s,%s,%s,%s,%s,%s,%s,%s\n");
   TString valueFormat_csv( "%s,%8ld,%s,%4.1%,%8ld,%4.1f%%,%8ld,%4.1f%%\n");
   
   fprintf(out, todaystring.Data());
   fprintf(out, dashFormat.Data(),dashes,dashes,dashes,dashes,dashes,dashes,dashes,dashes);
   fprintf(out, textFormat.Data(),"","Site","NJobs","Owner (Percent of Ownership)","Owner job","","WallHours","");
   fprintf(out, dashFormat.Data(),dashes,dashes,dashes,dashes,dashes,dashes,dashes,dashes);
   
   fprintf(outcsv, textFormat_csv.Data(),"Site","NJobs","Owner","Owner NJobs","Fraction Jobs","Owner Wall","Fraction Wall");

   int site_index = 1;
   int vo_index = 1;
   while (stmt->NextResultRow()) {

      std::string site = stmt->GetString(0);
      std::string owner = owners[site];
      InnerMap_t &owners = ownerShare[stmt->GetString(0)];
      long njobs = stmt->GetLong(1);
      long wall = stmt->GetDouble(2) / 3600;
      //double cpu = stmt->GetDouble(3);
      
      total_njobs += njobs;
      total_wall += wall;
      
      long owner_njobs = 0;
      long owner_wall = 0;
      double owner_cpu = 0;



      query = Form(pattern2,site.c_str(),owner.c_str(),owner.c_str());
      
      TSQLStatement *stmt2 = db->Statement(query);
      if (gDebugLevel >= 1) Info("Report","Running : %s\n",query.Data());
   
      if (owners.size()==0) {
         owners[owner].fVO = owner;
         owners[owner].fPercent = 100;
         owners[owner].fGuessed = true;
      }
      
      if (stmt2!=0) {

         stmt2->Process();
         stmt2->StoreResult();
      
         while (stmt2->NextResultRow()) {
            std::string vo = stmt2->GetString(0);
            std::transform(vo.begin(), vo.end(), vo.begin(), ::tolower);

            owner_njobs = stmt2->GetLong(1);
            owner_wall = stmt2->GetDouble(2) / 3600;
            owner_cpu = stmt2->GetDouble(3);
            
            owners[vo].fNjobs += owner_njobs;
            owners[vo].fWall += owner_wall;
            if (owners[vo].fVO.length()==0) {
               owners[vo].fVO = vo;
            }

            if (owners[vo].fPercent) {
               total_owner_njobs += owner_njobs;
               total_owner_wall += owner_wall;
            }
         }
         delete stmt2;
      }
      
      typedef InnerMap_t::iterator iter_t;
      
      std::multimap<int,OInfo > sortedlist;
      for (iter_t what = owners.begin() ;what != owners.end(); ++what) {
         sortedlist.insert( make_pair( -what->second.fPercent, what->second ) );
      }
      typedef std::multimap<int,OInfo >::iterator siter_t;
      
      int n_other_owners = 0;
      int njobs_other_owners = 0;
      long wall_other_owners = 0;
      for (siter_t what = sortedlist.begin() ;what != sortedlist.end(); ++what) {
         // Count the rest.
         
         if (what->second.fPercent != 0) {
            owner_njobs = what->second.fNjobs;
            owner_wall = what->second.fWall;
            std::string name( what->second.fVO );
            if (what->second.fGuessed) {
               name += " (not in OIM)";
            }
            
            fprintf(out,valueFormat.Data(),site_index, vo_index, site.c_str(), njobs,
                    name.c_str(), what->second.fPercent, owner_njobs, njobs ? 100.0*owner_njobs/njobs : 0.0, owner_wall, wall ? 100.0*owner_wall/wall : 0);
            
            fprintf(outcsv,valueFormat_csv.Data(),site.c_str(), njobs,
                    name.c_str(), what->second.fPercent, owner_njobs, njobs ? 100.0*owner_njobs/njobs : 0.0, owner_wall, wall ? 100.0*owner_wall/wall : 0);
            
            ++vo_index;
         } else {
            ++n_other_owners;
            njobs_other_owners += what->second.fNjobs;
            wall_other_owners += what->second.fWall;
         }
      }
      
      fprintf(out,valueFormat.Data(),site_index, vo_index, site.c_str(), njobs,
              Form("%d non-owning VOs",n_other_owners), 0, njobs_other_owners, njobs ? 100.0*njobs_other_owners/njobs : 0.0, wall_other_owners, wall ? 100.0*wall_other_owners/wall : 0);
      
      fprintf(outcsv,valueFormat_csv.Data(),site.c_str(), njobs,
              Form("%d non-owning VOs",n_other_owners), 0, njobs_other_owners, njobs ? 100.0*njobs_other_owners/njobs : 0.0, wall_other_owners, wall ? 100.0*wall_other_owners/wall : 0);
      fprintf(out, dashFormat.Data(),dashes,dashes,dashes,dashes,dashes,dashes,dashes,dashes);
      
      vo_index = 0;
      ++site_index;
   }
   fprintf(out,valueFormat.Data(),site_index,vo_index,"All", total_njobs, "", 100, total_owner_njobs,total_njobs ? 100.0*total_owner_njobs/total_njobs : 0.0, total_owner_wall, total_wall ? 100.0*total_owner_wall/total_wall : 0);
   fprintf(out, dashFormat.Data(),dashes,dashes,dashes,dashes,dashes,dashes,dashes,dashes);

   fprintf(outcsv,valueFormat_csv.Data(),"All", total_njobs, "", 100, total_owner_njobs,total_njobs ? 100.0*total_owner_njobs/total_njobs : 0.0, total_owner_wall, total_wall ? 100.0*total_owner_wall/total_wall : 0);

   delete stmt;
}

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

long getOneLong(TSQLServer *db, const char *query, int field ) 
{

   Info("Report","Running : %s\n",query);
   TSQLStatement *stmt = db->Statement(query);
   
   stmt->Process();
   stmt->StoreResult();

   long result = 0;
   if (stmt->NextResultRow()) {
      result = stmt->GetLong(field);
   }
   delete stmt;
   return result;
}

long getOneLong(TSQLServer *db, const char *sql, TDatime *begin, TDatime *end,int field = 0) 
{
   TString sbegin( begin->AsSQLString() );
   TString send( end->AsSQLString() );
   const char *query = Form(sql,sbegin.Data(),send.Data());

   return getOneLong(db,query,field);
}

double getOneDouble(TSQLServer *db, const char *sql, TDatime *begin, TDatime *end,int field = 0) 
{
   TString sbegin( begin->AsSQLString() );
   TString send( end->AsSQLString() );
   const char *query = Form(sql,sbegin.Data(),send.Data());

   return getOneDouble(db,query,field);
}

double getOneDouble(TSQLServer *db, const char *query, int field) 
{
   Info("Report","Running : %s\n",query);
   TSQLStatement *stmt = db->Statement(query);
   
   stmt->Process();
   stmt->StoreResult();

   double result = 0;
   if (stmt->NextResultRow()) {
      result = stmt->GetDouble(field);
   }
   delete stmt;
   return result;
}


TDatime* getdatime(const char *date) 
{
   tm *tmp = new tm();
   strptime(date,"%Y/%m/%d",tmp);
   TDatime *ret = new TDatime( mktime(tmp) );
   delete tmp;
   return ret;
}

void dategraph(TSQLServer *db, const char*sql, TDatime *begin, TDatime *end) 
{
   TString sbegin( begin->AsSQLString() );
   TString send( end->AsSQLString() );
   const char *query = Form(sql,sbegin.Data(),send.Data());

   // genericoutput(db,query);

   Info("Report","Running : %s\n",query);
   TSQLStatement *stmt = db->Statement(query);
   
   stmt->Process();
   stmt->StoreResult();

   std::vector<int> when,howmuch;

   //int nfields = stmt->GetNumFields();
   long offset = begin->Convert();
   while (stmt->NextResultRow()) {
      TDatime *w = getdatime( stmt->GetString(0) );
      long value = stmt->GetLong(1);
      when.push_back( w->Convert() - offset );
      howmuch.push_back( value );
      // fprintf(stderr,"... %d %d %d\n",when.size(),w->Convert(),value);
   }
   delete stmt;
   TGraph *gt2 = new TGraph(when.size(),&(when[0]),&(howmuch[0]));
   gt2->SetName("SiteReporting");

   gt2->SetTitle("Number of site reporting to Gratia");
   gt2->SetMinimum(0);

   gt2->Draw("AL");
   gt2->SetFillColor(19);
   gt2->SetMarkerColor(4);
   gt2->SetMarkerStyle(29);
   gt2->SetMarkerSize(1.3);
   // gt2->GetXaxis()->SetLabelSize(0.05);
   // Sets time on the X axis
   gt2->GetXaxis()->SetTimeOffset( begin->Convert() );
   gt2->GetXaxis()->SetLabelOffset(0.023);
   gt2->GetXaxis()->SetTimeDisplay(1);
   gt2->GetXaxis()->SetTimeFormat("#splitline{%Y}{%d /%m}");     
   //   gt2->Dump();

}

void process_lengthdensity(TSQLServer *db, const TString &query, TH1 *h, TH1*hmin, TH1*hsucc,TH1*hfail)
{
   Info("Report","Running : %s\n",query.Data());
   TSQLStatement *stmt = db->Statement(query.Data());
   
   stmt->Process();
   stmt->StoreResult();

   std::vector<int> what,howmuch;
   //int nfields = stmt->GetNumFields();
   while (stmt->NextResultRow()) {
      long value = stmt->GetLong(0); 
      long status = stmt->GetLong(1);
      long count = stmt->GetLong(2);
      what.push_back( value );
      howmuch.push_back( count );
      // fprintf(stderr,"... %d %d %d\n",when.size(),w->Convert(),value);
      h->Fill(value/3600,count);
      if (value<3600) hmin->Fill(value/60,count);
      if (value>3600) {
         if (status==0) hsucc->Fill(value/3600,count);
         else           hfail->Fill(value/3600,count);
      }
   }

    delete stmt;

}

void lengthdensity(TSQLServer *db,TString VOName,TDatime *begin, TDatime *end) {
   //   const char *sql1 = "SELECT WallDuration,count(*) FROM JobUsageRecord_Report J where VOName = \"%s\" and '%s'<=EndTime and EndTime < '%s' group by WallDuration";

   const char *sql2 = "SELECT WallDuration,R.Value as Status, count(*) FROM JobUsageRecord_Report J, Resource R where VOName = \"%s\" and '%s'<=EndTime and EndTime < '%s' and J.dbid = R.dbid and R.Description = \"ExitCode\" group by WallDuration,R.Value";

   const char *sql3 = "SELECT WallDuration,Status,count(*) FROM JobUsageRecord_Report J where VOName = \"%s\" and '%s'<=EndTime and EndTime < '%s' and J.StatusDescription != \"Condor Exit Status\" group by WallDuration,Status";
   
   TH1F *h = new TH1F("h","Job Length",100,0,10);
   TH1F *hfail = new TH1F("hfail","Job Length",100,0,10);
   TH1F *hmin = new TH1F("hmin","Job Length (under 1 hour)",60,0,60);

   h->SetBit(TH1::kCanRebin);
   h->SetYTitle("Number of Jobs");
   h->SetXTitle("Job Length in Hours");
   h->GetXaxis()->CenterTitle(true);
   h->GetYaxis()->CenterTitle(true);

   TH1F *hsucc = (TH1F*)h->Clone("hsucc");
   hsucc->SetYTitle("Number of Successful Jobs");
   
   hfail->SetBit(TH1::kCanRebin);
   hfail->SetYTitle("Number of Failed Jobs");
   hfail->SetXTitle("Job Length in Hours");
   hfail->GetXaxis()->CenterTitle(true);
   hfail->GetYaxis()->CenterTitle(true);

   hmin->SetBit(TH1::kCanRebin);
   hmin->SetYTitle("Number of Jobs");
   hmin->SetXTitle("Job Length in Minutes");
   hmin->GetXaxis()->CenterTitle(true);
   hmin->GetYaxis()->CenterTitle(true);

   TString sbegin( begin->AsSQLString() );
   TString send( end->AsSQLString() );

   TString query = Form(sql2,VOName.Data(),sbegin.Data(),send.Data());   
   process_lengthdensity(db,query,h,hmin,hfail,hsucc);
   
   query = Form(sql3,VOName.Data(),sbegin.Data(),send.Data());
   process_lengthdensity(db,query,h,hmin,hfail,hsucc);
   
   TCanvas *c = new TCanvas("c1", "c1",15,32,1094,762);
   c->Divide(1,2);
   c->cd(1);
   h->Draw();
   c->cd(2);
   hmin->Draw();

   c->SaveAs(Form("joblength_%s.jpg",VOName.Data()));

   TCanvas *c2 = new TCanvas("c2", "c2",15,32,1094,762);

   hsucc->SetFillColor(kBlue);
   hfail->SetFillColor(kRed);

   Double_t max_1 = hsucc->GetXaxis()->GetXmax();
   Double_t max_2 = hfail->GetXaxis()->GetXmax();
   
   if (max_1 > max_2) hfail->RebinAxis(max_1,hfail->GetXaxis());
   else               hsucc->RebinAxis(max_2,hsucc->GetXaxis());
   
   THStack *h2 = new THStack("h2","All jobs");
   h2->Add(hfail);
   h2->Add(hsucc);
   h2->Draw("");

   c2->SaveAs(Form("joblength_witherror_%s.jpg",VOName.Data()));

}   

TDatime* adddays(TDatime *in, Int_t days) {
   UInt_t time = in->Convert();
   time += days * 24*3600;
   in->Set(time);
   return in;
}

TDatime* gettoday() {
   TDatime *t = new TDatime();
   t->Set(t->GetDate(),00000);
   return t;
}

void genericoutput_format(TSQLServer *db, const char*sql) 
{
   TDatime *t = gettoday();
   TString end = t->AsSQLString();
   TString begin = adddays(t,-1)->AsSQLString();
   delete t;
   const char *what = Form(sql,begin.Data(),end.Data());
   return genericoutput(db,what);
}

void genericoutput(TSQLServer *db, const char*sql, TDatime *begin, TDatime *end) 
{
   TString sbegin( begin->AsSQLString() );
   TString send( end->AsSQLString() );
   const char *query = Form(sql,sbegin.Data(),send.Data());

   return genericoutput(db,query);
}



void DBInfo(TSQLServer *db) 
{
   TSQLRow *row;
   TSQLResult *res;
   
   // list databases available on server
   printf("\nList all databases on server %s\n", db->GetHost());
   res = db->GetDataBases();
   while ((row = res->Next())) {
      printf("%s\n", row->GetField(0));
      delete row;
   }
   delete res;

   // list tables in database "test" (the permission tables)
   printf("\nList all tables in database \"test\" on server %s\n",
          db->GetHost());
   res = db->GetTables("test");
   while ((row = res->Next())) {
      printf("%s\n", row->GetField(0));
      delete row;
   }
   delete res;
   
   // list columns in table "CEProbes" in database "gratia"
   printf("\nList all columns in table \"CEProbes\" in database \"gratia\" on server %s\n",
          db->GetHost());
   res = db->GetColumns("gratia", "CEProbes");
   while ((row = res->Next())) {
      printf("%s\n", row->GetField(0));
      delete row;
   }
   delete res;
}

void njobs(ostream &out, TSQLServer *db, TDatime *begin, TDatime *end)
{

   const char *sql = "select sum(NJobs) from JobUsageRecord where \"%s\"<EndTime and EndTime<\"%s\"";
   long njobs = getOneLong(db,sql,begin,end);
   out << "Yesterday, (from " << begin->AsString() << " to " << end->AsString() << ")\n<BR>\n";
   out << "There was " << njobs << " jobs that finished on the OSG sites reporting to Gratia\n<BR>\n";
}

void nsites(ostream &out, TSQLServer *db, TDatime *begin, TDatime *end)
{
   const char *sql = "SELECT date_format(EndTime,\"%%Y/%%m/%%d\") as day,count(distinct CETable.facility_name)  from gratia.CETable, gratia.CEProbes, gratia.VOProbeSummary J  where CEProbes.facility_id = CETable.facility_id and J.ProbeName = CEProbes.probename  and \"%s\"<EndTime and EndTime<\"%s\" and J.ProbeName not like \"psacct:%%\" group by day";

   long nsites = getOneLong(db,sql,begin,end,1);
   out << "Yesterday, (from " << begin->AsString() << " to " << end->AsString() << ")\n<BR>\n";
   out << "There was " << nsites << " OSG sites reporting to Gratia\n<BR>\n";
}


TSQLServer *getServer() {
   static TSQLServer *db = TSQLServer::Connect("mysql://gratia-db01.fnal.gov:3320/gratia","reader", "reader");
   static bool first = true;

   if (db==0) {
      Error("gratia","db not found");
      return 0;
   }
   if (first) {
      printf("Server info: %s\n", db->ServerInfo());
      first = false;
   }

   return db;
}

void gratia(int /* mode */ = 0)
{
   TSQLServer *db = getServer();  if (db==0) return;

   // start timer
   TStopwatch timer;
   timer.Start();

   // query database and print results
   //   const char *sql = "select dataset,rawfilepath from test.runcatalog "
   //                "WHERE tag&(1<<2) AND (run=490001 OR run=300122)";
//   const char *sql = "select count(*) from test.runcatalog "
//                     "WHERE tag&(1<<2)";
   
   const char *sql = "select * from CEProbes";

   // genericoutput(db,sql);

   TDatime *today = gettoday();
   TDatime *yesterday = adddays(gettoday(),-1);
 
   ofstream file;
   file.open("gratia_njobs.html", ios::out);
   njobs(file, db, yesterday, today);
   file.close();
   file.open("gratia_nsites.html", ios::out);
   nsites(file, db,yesterday,today);
   file.close();

   sql = "SELECT date_format(EndTime,\"%%Y/%%m/%%d\") as day,count(distinct CETable.facility_name)  from gratia.CETable, gratia.CEProbes, gratia.VOProbeSummary J  where CEProbes.facility_id = CETable.facility_id and J.ProbeName = CEProbes.probename  and \"%s\"<EndTime and EndTime<\"%s\" and J.ProbeName not like \"psacct:%%\" group by day";

   TDatime *b = adddays(gettoday(),-30);
   dategraph(db,sql,b,today);
   gPad->SaveAs("gratia_nsites.jpg");

   delete b;
   delete today;
   delete yesterday;
   delete db;

   // stop timer and print results
   timer.Stop();
   Double_t rtime = timer.RealTime();
   Double_t ctime = timer.CpuTime();

   printf("\nRealTime=%f seconds, CpuTime=%f seconds\n", rtime, ctime);
}

void sharing(const char *dirname=0, const char *when = 0) 
{
   TSQLServer *db = getServer();  if (db==0) return;
   TDatime *today = 0;
   if (when) {
      today = getdatime( when );
   } else {
      today = gettoday();
   }
   TDatime *yesterday = adddays(new TDatime(*today),-1);
   TDatime *daybefore = adddays(new TDatime(*today),-2);
 
   // const char *sql = "SELECT EndTime,VOName, Sum(Njobs),Sum(WallDuration),Sum(CPuUserDuration+CpuSystemDuration) FROM VOProbeSummary V where '%s' < EndTime and EndTime < '%s' group by EndTime,VOName";

   // genericoutput(db,sql,yesterday,today);

   TString txtname("share.txt");
   TString csvname("share.csv");
   FILE *txt,*csv;
   if (dirname==0) {
      txt = stdout;
      csv = stdout;
   } else {
      txt = fopen(gSystem->ConcatFileName(dirname,txtname),"w");
      csv = fopen(gSystem->ConcatFileName(dirname,csvname),"w");
   }
   sharing(txt, csv, db,yesterday,today);

   if (dirname!=0) {
      fclose(txt);
      fclose(csv);
   }

   delete today;
   delete yesterday;
   delete daybefore;
}

void runlengthdensity(TString VOName, int days = 1)
{
   TDatime *today = gettoday();   
   TDatime *yesterday = adddays(gettoday(),-days);

   lengthdensity(getServer(),VOName,yesterday,today);      

   delete today;
   delete yesterday;
}


void CpuPerWeek(TSQLServer *db, TDatime *begin, TDatime *end, const char *dir, int xsize, int ysize) 
{

   const char *sql = 
      "select date_format(VOProbeSummary.EndTime, '%Y-%U') as endtime,"
      "    sum(VOProbeSummary.WallDuration) as WallDuration"
      "  from VOProbeSummary"
      "  where (EndTime) >= date('%s') and (EndTime) < date('%s')  and ResourceType = 'Batch'"
      " group by date_format(VOProbeSummary.EndTime,'%Y-%U') order by VOProbeSummary.EndTime;";

   TString sbegin( begin->AsSQLString() );
   TString send( end->AsSQLString() );
   TString query = Form(sql,sbegin.Data(),send.Data());

   TSQLStatement *stmt = db->Statement(query);

   if (stmt==0) return;

   stmt->Process();
   stmt->StoreResult();

   typedef std::vector<std::pair<TString,double> > container;
   container data;

   TString week;
   while (stmt->NextResultRow()) {
      week = stmt->GetString(0);
      Double_t wall = stmt->GetULong64(1) / 3600.0;
      // cout << week  << " : " << wall << endl;
      data.push_back(make_pair(week,wall));
   }

   TH1D *histogram = new TH1D("h1","WallDuration Hours Per Week",data.size(),0,2000);
   TH1D *integral = new TH1D("h2","WallDuration Hours Per Week",data.size(),0,2000);
   histogram->SetBit(kCanDelete);
   integral->SetBit(kCanDelete);

   container::iterator iter;
   Double_t accum = 0;
   for(iter = data.begin(); iter != data.end(); ++iter) {
      accum += iter->second;
      histogram->Fill( iter->first.Data(), iter->second );
      integral->Fill( iter->first.Data(), accum );
   }
   integral->SetStats(0);
   histogram->SetStats(0);
   TCanvas *c1 = new TCanvas("c1", "c1", 0, 0, xsize, ysize);
   Int_t ci;   // for color index setting
   ci = TColor::GetColor("#99cccc");
   c1->SetFillColor(0);
   c1->SetBorderSize(2);
   c1->SetBottomMargin(0.1391753);
//    c1->SetFrameFillColor(0);
//    c1->SetFrameFillColor(19);

   //integral->Draw("L");
   histogram->SetMarkerColor(4);
   histogram->SetMarkerStyle(29);
   histogram->SetMarkerSize(1.3);
   histogram->GetXaxis()->SetLabelFont(22);
   histogram->Draw("L");
   
   c1->Update(); //Required for proper scaling

   //scale hint1 to the pad coordinates
   Float_t rightmax = 1.1*integral->GetMaximum();
   Float_t scale = c1->GetUymax()/rightmax;
   integral->SetLineColor(kBlue);
   integral->Scale(scale);
   integral->Draw("Lsame");

   //draw an axis on the right side
   TGaxis *axis = new TGaxis(c1->GetUxmax(),c1->GetUymin(),
                             c1->GetUxmax(),c1->GetUymax(),0,rightmax,510,"+L");
   axis->SetTitle("Cumulative");
   axis->CenterTitle();
   axis->SetLineColor(kBlue);
   axis->SetTextColor(kBlue);
   axis->Draw();

   delete stmt;

   c1->SaveAs(gSystem->ConcatFileName(dir,Form("WallPerWeek_%d.png",xsize)));
}



void VOWeek(TSQLServer *db, TDatime *begin, TDatime *end, const char *dir, int xsize, int ysize, Bool_t toponly = kFALSE) 
{

   const char *sql = 
      "select VOName, sum(WallDuration) as sumwall from VOProbeSummary"
      "   where EndTime >= date('%s') and EndTime < date('%s') " 
      "     and ResourceType = 'Batch'and VOName != 'Unknown' "
      "group by VOName "
      "order by sumwall desc, VOName     ";

   TString sbegin( begin->AsSQLString() );
   TString send( end->AsSQLString() );
   TString query = Form(sql,sbegin.Data(),send.Data());

   TSQLStatement *stmt = db->Statement(query);

   if (stmt==0) return;

   stmt->Process();
   stmt->StoreResult();

   typedef std::vector<std::pair<TString,double> > container;
   container data;

   TString week;
   Double_t max = 0;
   while (stmt->NextResultRow()) {
      week = stmt->GetString(0);
      Double_t wall = stmt->GetULong64(1) / 3600.0;
      // cout << week  << " : " << wall << endl;
      data.push_back(make_pair(week,wall));
      if (wall>max) max = wall;
   }

   Int_t size = data.size();
   if (toponly) {  
      size = 0;
      container::iterator iter;
      for(iter = data.begin(); iter != data.end(); ++iter) {
         if (iter->second > max*0.1) {
            ++size;
         }
      }
   }
 
   TH1D *histogram = new TH1D("h1","WallDuration Hours Per VO Last Week",size,0,2000);
   TH1D *integral = new TH1D("h2","WallDuration Hours Per VO Last Week",size,0,2000);
   histogram->SetBit(kCanDelete);
   integral->SetBit(kCanDelete);

   container::iterator iter;
   Double_t accum = 0;
   for(iter = data.begin(); iter != data.end(); ++iter) {
      if (!toponly || iter->second > max*0.1) {
         accum += iter->second;
         histogram->Fill( iter->first.Data(), iter->second );
         integral->Fill( iter->first.Data(), accum );
      }
   }
   integral->SetStats(0);
   histogram->SetStats(0);
   TCanvas *c1 = new TCanvas("c1", "c1", 0, 0, xsize, ysize);
   Int_t ci;   // for color index setting
   ci = TColor::GetColor("#99cccc");
   c1->SetFillColor(0);
   c1->SetBorderSize(2);
   c1->SetBottomMargin(0.1391753);
//    c1->SetFrameFillColor(0);
//    c1->SetFrameFillColor(19);

   histogram->SetMarkerColor(4);
   histogram->SetMarkerStyle(29);
   histogram->SetMarkerSize(1.3);
   histogram->GetXaxis()->SetLabelFont(22);
   histogram->Draw("");
   
   c1->Update(); //Required for proper scaling

   delete integral;

   delete stmt;

   c1->SaveAs(gSystem->ConcatFileName(dir,Form("WallVOLastWeek_%d.png",xsize)));
}


void SiteWeek(TSQLServer *db, TDatime *begin, TDatime *end, const char *dir, int xsize, int ysize, Bool_t toponly = kFALSE) 
{

   const char *sql = 
      "select Site.SiteName, sum(sub.WallDuration) from ( "
      "   select ProbeSummary.ProbeName, sum(ProbeSummary.WallDuration) as WallDuration "
      "   from ProbeSummary "
      "   where (EndTime) >= date('2007-01-10') and (EndTime) < date('2007-07-10')  and ResourceType = 'Batch' "
      "   group by ProbeSummary.ProbeName) as sub, Probe,Site "
      "where sub.ProbeName = Probe.ProbeName and Site.siteid = Probe.siteid "
      "group by Site.SiteName "
      "      order by Site.SiteName;";

   TString sbegin( begin->AsSQLString() );
   TString send( end->AsSQLString() );
   TString query = Form(sql,sbegin.Data(),send.Data());

   TSQLStatement *stmt = db->Statement(query);

   if (stmt==0) return;

   stmt->Process();
   stmt->StoreResult();

   typedef std::vector<std::pair<TString,double> > container;
   container data;

   TString week;
   Double_t max = 0;
   while (stmt->NextResultRow()) {
      week = stmt->GetString(0);
      Double_t wall = stmt->GetULong64(1) / 3600.0;
      // cout << week  << " : " << wall << endl;
      data.push_back(make_pair(week,wall));
      if (wall>max) max = wall;
   }

   Int_t size = data.size();
   if (toponly) {  
      size = 0;
      container::iterator iter;
      for(iter = data.begin(); iter != data.end(); ++iter) {
         if (iter->second > max*0.1) {
            ++size;
         }
      }
   }
 
   TH1D *histogram = new TH1D("h1","WallDuration Hours Site Last Week",size,0,2000);
   TH1D *integral = new TH1D("h2","WallDuration Hours Site Last Week",size,0,2000);
   histogram->SetBit(kCanDelete);
   integral->SetBit(kCanDelete);

   container::iterator iter;
   Double_t accum = 0;
   for(iter = data.begin(); iter != data.end(); ++iter) {
      if (!toponly || iter->second > max*0.1) {
         accum += iter->second;
         histogram->Fill( iter->first.Data(), iter->second );
         integral->Fill( iter->first.Data(), accum );
      }
   }
   integral->SetStats(0);
   histogram->SetStats(0);
   TCanvas *c1 = new TCanvas("c1", "c1", 0, 0, xsize, ysize);
   Int_t ci;   // for color index setting
   ci = TColor::GetColor("#99cccc");
   c1->SetFillColor(0);
   c1->SetBorderSize(2);
   c1->SetRightMargin(0.03);
   c1->SetBottomMargin(0.4);
//    c1->SetFrameFillColor(19);
//    c1->SetFrameFillColor(19);

   histogram->SetMarkerColor(4);
   histogram->SetMarkerStyle(29);
   histogram->SetMarkerSize(1.3);
   histogram->GetXaxis()->SetLabelFont(22);
   histogram->GetYaxis()->SetLabelSize(0.03);
   histogram->Draw("");
   
   c1->Update(); //Required for proper scaling

   delete integral;

   delete stmt;

   c1->SaveAs(gSystem->ConcatFileName(dir,Form("WallSiteLastWeek_%d.png",xsize)));
}

void runCpuPerWeek(const char *dir, int xsize, int ysize) {
   TDatime *today = gettoday();   
   TDatime *lastyear = adddays(gettoday(),-365);

   CpuPerWeek(getServer(),lastyear,today,dir,xsize,ysize);

   delete today;
   delete lastyear;
}

void runVOWeek(const char *dir, int xsize, int ysize) {
   TDatime *today = gettoday();   
   TDatime *lastweek = adddays(gettoday(),-7);

   VOWeek(getServer(),lastweek,today,dir,xsize,ysize);

   delete today;
   delete lastweek;
}

void runSiteWeek(const char *dir, int xsize, int ysize) {
   TDatime *today = gettoday();   
   TDatime *lastweek = adddays(gettoday(),-7);

   SiteWeek(getServer(),lastweek,today,dir,xsize,ysize);

   delete today;
   delete lastweek;
}

void runOSG(const char *topdir, int xsize, int ysize) {
   TString dir = gSystem->ConcatFileName(topdir,"osg_gratia_display");
   TString giffile(Form("osggratia_%d.gif",xsize));
   giffile = gSystem->ConcatFileName(dir,giffile);

   TString gifadd = giffile + "+300";
   TString gifaddend = giffile + "++300";
   
   gSystem->Unlink(giffile);

   gROOT->SetStyle("Plain");

   runCpuPerWeek(dir,xsize,ysize);
   gPad->Print(gifadd);
   delete gPad;

   runVOWeek(dir,xsize,ysize);
   gPad->Print(gifadd);
   delete gPad;

   runSiteWeek(dir,xsize,ysize);
   gPad->Print(gifaddend);
   delete gPad;

   
}
