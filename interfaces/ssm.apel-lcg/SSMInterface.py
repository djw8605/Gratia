#!/usr/bin/python

import commands, os, sys, time, string
import popen2
import getopt
import glob
import traceback
import exceptions
import ConfigParser

class SSMException(Exception):
  pass

class SSMInterface:
  """ Author: John Weigand (11/17/11)
      Description:
        This class retrieves reads the SSM configuration file
        and provides various methods for viewing/using the data.
        This is something that should be provided by APEL/SSM.
  """

  #############################
  def __init__(self,config_file,ssm_home):
    self.config = ConfigParser.ConfigParser()
    if not os.path.isfile(config_file):
      raise SSMException("""The SSM configuration file does not exist (%s).""" % config_file)
    self.configFile = config_file
    self.config.read(config_file)
    self.ssm_master = "%s/ssm/ssm_master.py" % ssm_home
    self.outgoing = "%s/outgoing" % self.config.get("messagedb","path")
    self.__validate__() 

  #-----------------------
  def __validate__(self):
    if not os.path.isdir(self.outgoing):
      raise SSMException("""The outgoing messages directory defined by the SSM configuration file:
  %(config)s 
is not a directory: %(outgoing)s
This is the "messagedb" section, "path" attribute""" % \
       { "config" : self.configFile, "outgoing" : self.outgoing })
    #-- verify ssm_master exists --
    if not os.path.isfile(self.ssm_master):
      raise SSMException("""The main interface file does not exist:
  %(ssm_master)s""" % { "ssm_master" : self.ssm_master }) 

  #-----------------------
  def show_outgoing(self):
    """Display files in the SSM outgoing messages directory."""
    if self.outgoing_sent():
      return """There are no outgoing messages in:
  %s""" %  self.outgoing  
    else: 
      return """Outgoing messages in %(dir)s
%(files)s""" %  { "dir" : self.outgoing, "files" : os.listdir(self.outgoing) }  

  #-----------------------
  def outgoing_sent(self):
    """ Checks to see if any files are in the SSM outgoing directory.
        Returns: True  if there are no files in the directory.
                 False if there are files in the directory.
    """
    msgs = glob.glob(self.outgoing + '/*')
    if len(msgs) == 0:
      return True
    return False

  #-----------------------
  def send_outgoing(self,file):
    if not os.path.isfile(file):
      raise SSMException("File to be sent does not exist: %s" % file)
    os.system("cp %(file)s %(outgoing)s" % \
                 { "file" : file, "outgoing": self.outgoing} )
    if not os.path.isfile(file):
      raise SSMException("""Copy failed. File: %(file)s
To: %(dir)s""" % { "file" : file, "dir" : self.outgoing })

    cmd = "python %(ssm_master)s %(config)s" % \
       { "ssm_master" : self.ssm_master, "config" : self.configFile }
    p = popen2.Popen3(cmd,True)

    maxtime   = 120   # max seconds to wait before giving up and terminating job
    totaltime = 0
    sleep     = 10    # sleep time between checks to see if file was sent
    rtn = p.poll()
    while self.outgoing_sent() == False:
      rtn = p.poll()
      if rtn != -1:
        break
      os.system("sleep %s" % sleep)
      if totaltime > maxtime:
        self.kill_ssm(p.pid)
        raise SSMException("""Interface FAILED.  
Command: %(cmd)s
Had to kill process after %(timeout)s seconds.
Files in: %(dir)s
%(files)s
""" % { "timeout" : maxtime, "dir"   : self.outgoing, 
        "cmd"     : cmd,     "files" : os.listdir(self.outgoing) } )
      totaltime = totaltime + sleep
      
    if rtn > 0: 
      msg = """Interface FAILED.
Command:  %(cmd)s
""" %  { "cmd" : cmd }
      for line in p.fromchild.readlines():
        msg += line
      for line in p.childerr.readlines():
        msg += line
      raise SSMException(msg)
    self.kill_ssm(p.pid)
    
  #-----------------------
  def kill_ssm(self,pid):
    os.system("kill -9 %s >/dev/null 2>&1" % pid)

    
## end of class ###

#----------------
def usage():
  global gProgramName
  print """
Usage: %(program)s --config <SSM config file> Actions [-help]

  Provides visibility into SSM interface information.

  --config <SSM config file>
    This is the SSM configuration file used by the interface.

  Actions:
    --show-outgoing
        Displays the outgoing SSM messages directory contents.
"""

#----------------
def main(argv):
  global gProgramName
  gProgramName = argv[0]
  config = None

  action = ""
  type   = ""
  arglist = [ "help", "config=", "show-outgoing", "send-file=" ]
  try:
    opts, args = getopt.getopt(argv[1:], "", arglist)
    if len(opts) == 0:
      usage()
      print "ERROR: No command line arguments specified"
      return 1
    for o, a in opts:
      if o in ("--help"):
        usage()
        return 1
      if o in ("--show-outgoing", ):
        action = o
        continue
      if o in ("--send-file"):
        action = o
        file   = a
        continue
      if o in ("--config"):
        action = o
        config = a
        continue

    if config == None:
      usage()
      print "ERROR: you need to specify the --config option"
      return 1
    ssm = SSMInterface(config)
    if action == "--show-outgoing":
      print ssm.show_outgoing()
    elif action == "--send-file":
      ssm.send_outgoing(file)
    else:
      usage()
      print "ERROR: no action options specified"
      return 1

  except getopt.error, e:
    msg = e.__str__()
    usage()
    print "ERROR: Invalid command line argument: %s" % msg
    return 1
  except SSMException,e:
    print e
    return 1
  except Exception,e:
    traceback.print_exc()
    return 1
  return 0

######################################
#### MAIN ############################
######################################
if __name__ == "__main__":
  sys.exit(main(sys.argv))

