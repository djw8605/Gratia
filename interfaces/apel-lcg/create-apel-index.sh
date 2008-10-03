#!/bin/bash
#######################################################
# John Weigand (9/29/08)
#
# Creates an index.html page for the Gratia-APEL
# interface data for each month.
#######################################################
function usage {
  echo "
Usage: $PGM collector_instance 

This script will create an index.hmtl file in the 
<collector_instance>/webapps/gratia-data/interfaces/gratia-apel
directory for the following files generated by the Gratia-APEL interface 
script (LCG.py).

Currently, these files SQL selects of the 3 tables that Gratia has
visiblity to:
  OSG_DATA
  org_Tier1
  org_Tier2

It is looking for both .html and .xml suffixed files in the format
  YYYY-MM.<table_name>.<xml | html>

   collector_instance - Specify the full path of the Gratia tomcat
                        instance directory.  (e.g., /data/tomcat-gratia) 

This script assumes the xml/html files exist in 
<collector_instance>/webapps/gratia-data/interfaces/apel-lcg.
" 
}
#--------------------
function logerr { echo "
ERROR: $1
";exit 1 
}
#--------------------
function write_file { echo "$1" >>$index
}
#---------------------
function start { write_file "<$1>"
}
function end { write_file "</$1>"
}
#---------------------
function header { 
  write_file "<TITLE>$1</TITLE>"
  write_file "<HEAD><CENTER><H3>$1</H3></CENTER></HEAD>"
}
#---------------------
function table { write_file "<TABLE $1>"
}
#---------------------
function row { write_file "<TR $1>"
}
#---------------------
function columnHREF { 
    write_file "<TD><CENTER><a href=\"$1\">${formatStart}${2}${formatEnd}</a></CENTER></TD>"
}
#---------------------
function column { 
  if [ -z "$1" ];then
    write_file "<TD $2>.</TD>"
  else
    write_file "<TD $2><CENTER>${formatStart}${1}${formatEnd}</CENTER></TD>"
  fi
}
#---------------------
function text { write_file "$1 "
}
function textbold { write_file "<b>$1 </b>"
}
#---------------------
function font { 
  write_file "<font color=white><b>$1</b></font>"
}
#---------------------
function write_description {
 cat >>$index <<EOF
The intent of this page is to provide visibility into the APEL accounting
database of the Gratia data and the tables affecting how the data is 
presented in the EGEE portal.
A full description of the Gratia-APEL interface and the tables is the
<a href="https://twiki.grid.iu.edu/bin/view/Accounting/GratiaInterfacesApelLcg">Gratia Interfaces - APEL/WLCG document</a>.
<p/>
The table below contains SQL select dumps of the tables in html and xml 
format that can be referenced for troubleshooting purposes.
<p>
EOF
}
#---------------------
function write_nebraska_description {
 cat >>$index <<EOF
Additional information related to WLCG Tier 1 and Tier 2 CMS and ATLAS Gratia 
data can be seen 
<a href="https://t2.unl.edu/gratia/wlcg_reporting">here</a>.  These pages will
show:
<ol>
<li> WLCG Accounting Summaries</li>
<li> Site Availability Measurements</li>
<li> GIP subcluster data including SpecInt values used</li>
<li> Site normalization factors used</li>
</ol>
<p>
EOF
}
### MAIN #############################
PGM=$(basename $0)
path=gratia-data/interfaces/apel-lcg
TOMCAT_INSTANCE=""
url="./"

#---------------------------------
# validate command line arguments 
#---------------------------------
case $1 in
 "\?"               ) usage;exit 1;;
 "-h"    | "--h"    ) usage;exit 1;;
 "-help" | "--help" ) usage;exit 1;;
esac

TOMCAT_INSTANCE=$1
if [ -z "$TOMCAT_INSTANCE" ];then
  usage;logerr "The collector_instance not specified"
fi
if [ ! -d "$TOMCAT_INSTANCE" ];then
  logerr "The collector_instance ($TOMCAT_INSTANCE) does not exist."
fi

datadir=$TOMCAT_INSTANCE/webapps/$path
if [ ! -d "$datadir" ];then
  logerr "The Gratia-APEL interface directory: 
  $datadir
does not exist or does not have the correct permissions."
fi

#--------------------
# Get started
#--------------------
cd $datadir
index=$datadir/$PGM.tmp.html
rm -f $index

#----------------
# Find the dates
#----------------
dates="$(ls *.xml *.html |egrep -v index.html|cut -d'.' -f1|sort -ur)"

#--------------------------
# Create the index file
#--------------------------
start HTML
header "GRATIA-APEL WLCG Interface"
start "BODY BGCOLOR=#CCCCCC"
text '<hr width=60%>'
start p
write_description
start p

#--------------------------------------------
# Create table for files/links 
#--------------------------------------------
start CENTER
formatStart="<font color=white><b>"
formatEnd="</b></font>"
start "TABLE BORDER=1"
##--- header row --
start "TR BGCOLOR=black"
column "Month"
column "OSG_DATA"  "colspan=2"
column "org_Tier1" "colspan=2"
column "org_Tier2" "colspan=2"
end TR
## -- new row ---
formatStart="<font color=black><b>"
formatEnd="</b></font>"
start "TR BGCOLOR=beige"
for date in $dates
do
  column "$date"
  for table in OSG_DATA org_Tier1 org_Tier2
  do
    for type in html xml
    do
      file=$date.$table.$type
      if [ -f $file ];then
        columnHREF "$url/$file" $type
      else
        column "n/a"
      fi
    done
  done
  end TR
done
end TABLE
end CENTER
start p
text '<hr width=60%>'
start p
write_nebraska_description
text '<hr width=60%>'
text "Last updated: $(date)"

end body;end html

mv $index index.html
rm -f $index
exit 0
