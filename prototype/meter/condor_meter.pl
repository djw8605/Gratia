#!/usr/bin/perl -w
#
# condor_meter.pl - Prototype for an OSG Accouting 'meter' for Condor
#       By Ken Schumacher <kschu@fnal.gov> Began 5 Nov 2005
# $Id: condor_meter.pl,v 1.1 2006-02-03 17:22:14 kschu Exp $
# Full Path: $Source: /var/tmp/move/gratia/prototype/meter/condor_meter.pl,v $
#
# Revision History:
#     5 Nov 2005 - Initial concept

# The name of one or more condor log files should be passed as
# parameters.  This script will parse the log and generate records which
# include the grid accounting data.

#==================================================================
# Variable definitions
#==================================================================

use English;   # For readability
use strict 'refs', 'subs';
use FileHandle;

$progname = "condor_meter.pl";
$prog_version = "v0.3";
$prog_revision = '$Revision: 1.1 $ ';   # CVS Version number
#$true = 1; $false = 0;
$verbose = 0;

#==================================================================
# Subroutine Definitions
#==================================================================

#------------------------------------------------------------------
# Subroutine NumSeconds ($time_string, $num_seconds)
#   This routine will convert a string (ie. 0 05:50:41) to a number of
#   seconds.
#
# I may add the reverse conversion here.  If I pass a null $time_string
# I could create a similar format time string from the passed
# $num_seconds.
# ------------------------------------------------------------------
sub NumSeconds {
  my $time_string = $_[0];
  my $num_seconds = $_[1];
  my $num_hours = $num_mins = 0;
  my $days = $hours = $mins = $secs = 0;

  if ($time_string =~ /(\d) (\d{2}):(\d{2}):(\d{2})/) {
    $days = $1;  $hours = $2;  $mins = $3; $secs = $4;
    $num_hours = ($days * 24) + $hours;
    $num_mins = ($num_hours * 60) + $mins;
    $num_seconds = ($num_mins * 60) + $secs;

    return $num_seconds
  } else {
    warn "Invalid time string: $time_string\n";
    return -1;
  }
}

#------------------------------------------------------------------
# Subroutine Feed_Gratia ($hash_ref)
#   This routine will take a hash of condor log data and push that
# data out to Gratia.
#------------------------------------------------------------------
sub Feed_Gratia {
  my %hash = @_ ;

  if ($verbose) {
    print "Feed_Gratia was passed Cluster_id of $hash{'ClusterId'}\n";
  }

  $py = new FileHandle;
  $py->open("| python");
  print $py "import Gratia\n";
  print $py "Gratia.Initialize()\n";
  print $py "r = Gratia.UsageRecord()\n";
	
  # 2.1 RecordIdentity must be set by Philippe's module?
  # 2.2 GlobalJobId
  print $py qq/r.GlobalJobId(\"/ . $hash{'UniqGlobalJobId'} . qq/\")\n/;
  # 2.3 LocalJobId
  print $py qq/r.LocalJobId(\"/ . $hash{'ClusterId'} . qq/\")\n/;

  # 2.4 ProcessId - optional, integer
  # I'll have to parse this out of 'LastClaimId'
  #      sample: LastClaimId = "<131.225.167.210:32806>#1121113508#5671"
  if ($hash{'LastClaimId'} =~ /<.*:(\d*)>/) {
    $condor_process_id = $1;
  } else {
    $condor_process_id = 0;
  }
  if ($verbose) {
    print "From ($hash{'LastClaimId'})" .
      "I got process id ($condor_process_id)\n";
  }
  print $py "r.ProcessId(" . $condor_process_id . ")\n";

  # 2.5 LocalUserId
  print $py qq/r.LocalUserId(\"/ . $hash{'Owner'} . qq/\")\n/;
  # 2.6 GlobalUsername - such as the distinguished name from the certificate
  #      sample: User = "sdss@fnal.gov" is part of condor_history
  # print $py qq/r.GlobalUsername(\"/ . $hash{'User'} . qq/\")\n/;
  # 2.7 JobName - Condors name? for this job?
  print $py qq/r.JobName(\"/ . $hash{'GlobalJobId'} . qq/\")\n/;
  # 2.8 Charge - optional, integer, site dependent
  # 2.9 Status - optional, integer, exit status
  print $py qq/r.Status(\"/ . $hash{'ExitStatus'} . qq/\")\n/;
  # 2.10 WallDuration
  print $py qq/r.WallDuration(int(/ . $hash{'RemoteWallClockTime'} .
    qq/),\"Was entered in seconds\")\n/;
  # 2.11 CpuDuration - summed over all processes in the job
  print $py qq/r.CpuDuration(int(/ . $hash{'JobCpuSecsTotal'} .
    qq/),\"Was entered in seconds\")\n/;
  # 2.12 EndTime - optional, timestamp
  print $py qq/r.EndTime(/ . $hash{'CompletionDate'} .
    qq/,\"Was entered in seconds\")\n/;
  # 2.13 StartTime - optional, timestamp
  print $py qq/r.StartTime(/ . $hash{'JobStartDate'} .
    qq/,\"Was entered in seconds\")\n/;

  # ?.?? TimeDuration - for Condor that's RemoteUserCpu
  print $py qq/r.TimeDuration(/ . $hash{'RemoteUserCpu'} .
    qq/, \"RemoteUserCpu\")\n/;
  # ?.?? TimeInstant - ???

  # Parse the Submit hostname
  #      sample: GlobalJobId = "fngp-osg.fnal.gov#1124148654#4713.0"
  if ($hash{'GlobalJobId'} =~ /(.*)\#\d*\#.*/) {
    $condor_submit_host = "$1";
  } else {
    $condor_submit_host = "unknown submit host";
  }
  if ($verbose) {
    print "From ($hash{'GlobalJobId'})" .
      "I got submit host ($condor_submit_host)\n";
  }

  # 2.14 MachineName - can be host name or the sites name for a cluster
  print $py qq/r.MachineName(\"/ . $condor_submit_host . qq/\")\n/;

  # Host must be type DomainName so I must strip any reference to a
  #   virtual machine (which MIGHT be present)
  #      sample: LastRemoteHost = "vm1@fnpc210.fnal.gov"
  if ($hash{'LastRemoteHost'} =~ /vm\d+?\@(.*)/) {
    $fqdn_last_rem_host = "$1";
  } else {
    $fqdn_last_rem_host = $hash{'LastRemoteHost'};
  }
  if ($verbose) {
    print "From ($hash{'LastRemoteHost'})" .
      "I got submit host ($fqdn_last_rem_host)\n";
  }

  # 2.15 Host - hostname where the job ran and boolean for Primary
  print $py qq/r.Host(\"/ . $fqdn_last_rem_host . qq/\",True)\n/;
  # 2.16 SubmitHost - hostname where the job was submitted
  print $py qq/r.SubmitHost(\"/ . $condor_submit_host . qq/\")\n/;

  # 2.17 - Queue - string, name of the queue from which job executed
  #    I have a field called JobUniverse under Condor
  #      sample: JobUniverse = 5
  print $py qq/r.Queue(\"/ . $hash{'JobUniverse'} . 
    qq/\", \"Condor's JobUniverse field\")\n/;
  # 2.18 - ProjectName - optional, effective GID (string)
         # I am unsure if this should be the AccountingGroup below ###
  # 2.19 - Network - optional, integer
  # 2.20 - Disk - optional, integer, disk storage used, may have 'type'
  # 2.21 - Memory - optional, integer, mem use by all concurrent processes
  # 2.22 - Swap - optional, integer
  # 2.23 - NodeCount - optional, positive integer - physical nodes
  print $py qq/r.MachineName(\"/ . $hash{'MaxHosts'} . qq/\")\n/;
  # 2.24 - Processors - optional, positive integer - processors used/requested
  # 2.25 - ServiceLevel - optional, string (referred to as record identity?)
		
  # To use r.AddAdditionalInfo: record.AddAdditionalInfo("name",value)
  #    where value can be a string or number
  print $py qq/r.AdditionalInfo(\"CondorMyType\", \"/ 
    . $hash{'MyType'} . qq/\")\n/;
  print $py qq/r.AdditionalInfo(\"LocalUserCpu\", \"/ 
    . $hash{'LocalUserCpu'} . qq/\")\n/;
  print $py qq/r.AdditionalInfo(\"LocalSysCpu\", \"/ 
    . $hash{'LocalSysCpu'} . qq/\")\n/;
  print $py qq/r.AdditionalInfo(\"RemoteUserCpu\", \"/ 
    . $hash{'RemoteUserCpu'} . qq/\")\n/;
  print $py qq/r.AdditionalInfo(\"RemoteSysCpu\", \"/ 
    . $hash{'RemoteSysCpu'} . qq/\")\n/;
  print $py qq/r.AdditionalInfo(\"CumulativeSuspensionTime\", \"/ 
    . $hash{'CumulativeSuspensionTime'} . qq/\")\n/;
  print $py qq/r.AdditionalInfo(\"CommittedTime\", \"/ 
    . $hash{'CommittedTime'} . qq/\")\n/;
  #      sample: AccountingGroup = "group_sdss.sdss"
  print $py qq/r.AdditionalInfo(\"AccountingGroup\", \"/ 
    . $hash{'AccountingGroup'} . qq/\")\n/;
  print $py qq/r.AdditionalInfo(\"ExitBySignal\", \"/ 
    . $hash{'ExitBySignal'} . qq/\")\n/;
  print $py qq/r.AdditionalInfo(\"ExitCode\", \"/ 
    . $hash{'ExitCode'} . qq/\")\n/;
  print $py qq/r.AdditionalInfo(\"condor.JobStatus\", \"/ 
    . $hash{'JobStatus'} . qq/\")\n/;
  #print $py qq/r.AdditionalInfo(\"\", \"/ . $hash{''} . qq/\")\n/;

  print $py "#\n";
  print $py "# populate r\n";
  print $py "Gratia.Send(r)\n";

  $py->close;
}

#------------------------------------------------------------------
# Subroutine Query_Condor_History
#   This routine will call 'condor_history' to gather additional
# data needed to report this job's accounting data
#------------------------------------------------------------------
sub Query_Condor_History {
  my $cluster_id = $_[0];
  my $record_in;
  my %condor_hist_data;

  # my $condor_hist_cmd = "/export/osg/grid/condor/bin/condor_history";
  my $condor_hist_cmd = "/opt/condor-6.7.13/bin/condor_history";

  open(CHIST, "$condor_hist_cmd -l $cluster_id |")
    or die "Unable to open condor_history pipe\n";

  unless (defined($header = <CHIST>)) { die "Failed to get condor_history\n"; }

  #Test the first line returned to be sure the history command worked
  unless ($header =~ /\(ClusterId == (\d+)\)/ && $cluster_id == $1) {
    die "Invalid condor history returned ($header)\n";
  }

  #Load the remaining lines into a hash
  while ($record_in = <CHIST>) {
    if ($record_in =~ /(\S+) = (.*)/) {
      $element = $1;  $value=$2;

      # Strip double quotes where needed
      if ($value =~ /"(.*)"/) {
	$value = $1;
      }
      $condor_hist_data{$element} = $value;
    } else {
      if ($record_in =~ /\S+/) {
	warn "Could not parse: $record_in\n";
      }
    }
  }

  $condor_hist_data{'UniqGlobalJobId'} = 'condor.' . 
    $condor_hist_data{'GlobalJobId'};
  if ($verbose) {
    print "Unique ID: $condor_hist_data{'UniqGlobalJobId'}\n";
    #print "Query_Condor_History recorded Cluster_id of $condor_hist_data{'ClusterId'}\n";
  }

  return %condor_hist_data;
} # End of subroutine Query_Condor_History

#------------------------------------------------------------------
# Subroutine Process_005
#   This routine will process a type 005 termination record
#
# Sample '005 Job terminated' event record
# 005 (10078.000.000) 10/18 17:47:49 Job terminated.
#         (1) Normal termination (return value 1)
#                 Usr 0 05:50:41, Sys 0 00:00:11  -  Run Remote Usage
#                 Usr 0 00:00:00, Sys 0 00:00:00  -  Run Local Usage
#                 Usr 0 05:50:41, Sys 0 00:00:11  -  Total Remote Usage
#                 Usr 0 00:00:00, Sys 0 00:00:00  -  Total Local Usage
#         0  -  Run Bytes Sent By Job
#         0  -  Run Bytes Received By Job
#         0  -  Total Bytes Sent By Job
#         0  -  Total Bytes Received By Job
# ...
#------------------------------------------------------------------
sub Process_005 {
  my $filename = shift;
  my @term_event = @_;
  my $next_line = "";
  my $return_value = 0;
  my %condor_acctg_data;

  # Extract values from the ID line --------------------------------
  $id_line = shift @term_event;

  unless ($id_line =~ /005\s(\S+)\s(\S+)\s(\S+)/) {
    warn "Error parsing the 'Job terminated' record:\n$id_line";
    return 0;
  }
  $job_id = $1; $end_date = $2; $end_time = $3;
  if ($verbose) {
    print "from $id_line: I got id $job_id which ended $end_date at $end_time\n";
  }

  unless ($job_id =~ /\((\d+)\.(\d+)\.(\d+)\)/) {
    warn "Error parsing the 'Job id' field: $job_id";
    return 0;
  }
  $cluster_id = $1; # $cluster_field2 = $2; $cluster_field3 = $3;
  if ($verbose) {
    print "from $job_id: I got ClusterId $cluster_id\n";
  }

  # Next line indicates what job termination returned --------------
  $next_line = shift @term_event;
  if ($next_line =~ /Normal termination/) {
    # This was a Normal termination event
    if ($next_line =~ /return value (\d*)/) {
      $return_value = $1;
      print "\n$filename: Cluster_Id $cluster_id had return value: $return_value\n";
    } else {
      print "Malformed termination record:\n";
      print "$next_line";
    }
  } else {
    print "Event was not a Normal Termination\n";
    print "$next_line";
  }

  # The next four lines have CPU usage data ------------------------
  $next_line = shift @term_event;
  if ($next_line =~ /Usr (\d ..:..:..), Sys (\d ..:..:..).*Run Remote/) {
    $rem_usr_cpu = $1;  $rem_sys_cpu = $2;
    $rem_usr_cpusecs = NumSeconds($rem_usr_cpu);
    $rem_sys_cpusecs = NumSeconds($rem_sys_cpu);
  }

  $next_line = shift @term_event;
  if ($next_line =~ /Usr (\d ..:..:..), Sys (\d ..:..:..).*Run Local/) {
    $lcl_usr_cpu = $1;  $lcl_sys_cpu = $2;
    $lcl_usr_cpusecs = NumSeconds($lcl_usr_cpu);
    $lcl_sys_cpusecs = NumSeconds($lcl_sys_cpu);
  }

  $rem_cpusecs = $rem_usr_cpusecs + $rem_sys_cpusecs;
  $lcl_cpusecs = $lcl_usr_cpusecs + $lcl_sys_cpusecs;

  $next_line = shift @term_event;
  if ($next_line =~ /Usr (\d ..:..:..), Sys (\d ..:..:..).*Total Remote/) {
    $rem_usr_cpu_total = $1;  $rem_sys_cpu_total = $2;
    $rem_usr_cpusecs_total = NumSeconds($rem_usr_cpu_total);
    $rem_sys_cpusecs_total = NumSeconds($rem_sys_cpu_total);
  }

  $next_line = shift @term_event;
  if ($next_line =~ /Usr (\d ..:..:..), Sys (\d ..:..:..).*Total Local/) {
    $lcl_usr_cpu_total = $1;  $lcl_sys_cpu_total = $2;
    $lcl_usr_cpusecs_total = NumSeconds($lcl_usr_cpu_total);
    $lcl_sys_cpusecs_total = NumSeconds($lcl_sys_cpu_total);
  }

  $rem_cpusecs_total = $rem_usr_cpusecs_total + $rem_sys_cpusecs_total;
  $lcl_cpusecs_total = $lcl_usr_cpusecs_total + $lcl_sys_cpusecs_total;

  # Now print only the significant results
  print "Remote task CPU Duration: $rem_cpusecs seconds"
    . "($rem_usr_cpusecs/$rem_sys_cpusecs)\n"
      if ($rem_cpusecs && $rem_cpusecs != $rem_cpusecs_total);
  print "Local task CPU Duration: $lcl_cpusecs seconds"
    . "($lcl_usr_cpusecs/$lcl_sys_cpusecs)\n" 
      if ($lcl_cpusecs && $lcl_cpusecs != $lcl_cpusecs_total);
  print "Remote CPU Duration: $rem_cpusecs_total seconds"
    . "($rem_usr_cpusecs_total/$rem_sys_cpusecs_total)\n";
  print "Local CPU Duration: $lcl_cpusecs_total seconds"
    . "($lcl_usr_cpusecs_total/$lcl_sys_cpusecs_total)\n"
      if ($lcl_cpusecs_total);

  %condor_acctg_data = Query_Condor_History($cluster_id);
  if ($verbose) {
    print "Query_Condor_History returned GlobalJobId of $condor_acctg_data{'GlobalJobId'}\n";
  }

  $condor_acctg_data{'RemCpuSecsTotal'} = $rem_cpusecs_total;
  $condor_acctg_data{'JobCpuSecsTotal'} = $rem_cpusecs_total + $lcl_cpusecs_total;

  Feed_Gratia(%condor_acctg_data);

  return %condor_acctg_data;
}

#==================================================================
#  Main program block
#==================================================================

unless (@ARGV) {
  print "$progname version $prog_version ($prog_revision)\n\n";
  print "USAGE: $0 filename [ filename . . .]\n";
  exit 1;
}

#------------------------------------------------------------------
# Get source file name(s)

foreach $logfile (@ARGV) {
  open(LOGF, $logfile)
    or die "Unable to open logfile: $logfile\n";
  if ($verbose) { print "Processing file: $logfile\n"; }

  while ($record_in = <LOGF>) {
    @event_records = (); %condor_data_hash = ();
    #Do I need to pass the $record_in header as well?
    push @event_records, $record_in;
    if ($new_record = <LOGF>) {
      until ($new_record =~ /^\.\.\./) {
	push @event_records, $new_record;
	$new_record = <LOGF>;
      }
      # Locate the beginning of a '005 Job Terminated' event
      if ($record_in =~ /^005/) {
	
	%condor_data_hash = Process_005($logfile, @event_records);
	if ($verbose) {
	  print "Process_005 returned Cluster_id of $condor_data_hash{'ClusterId'}\n";
	}
      }
    } else {
      warn "$logfile: Improperly terminated event\n";
    }
  }

  close(LOGF);
}

#------------------------------------------------------------------
# Wrap up

  if ($verbose) {
    print "\nEnd of program: $progname\n";
  }

exit 0;

#==================================================================
# End of Program - condor_meter-pl
#==================================================================

#==================================================================
# CVS Log
# $Log: not supported by cvs2svn $

# Variables defined for EMACS editor usage
# Local Variables:
# mode:perl
# comment-start: "# "
# End:

