
"""
Module for interacting with Gratia.

Connects to the database,
Queries the database,
Summarizes resulting queries.
"""

import logging

import gold
import transaction

import MySQLdb

log = logging.getLogger("gratia_gold.gratia")

MAX_ID = 100000

QUERY = \
"""
SELECT
  max(JUR.dbid) as dbid,
  ResourceType,
  ReportableVOName,
  LocalUserId,
  sum(Charge) as Charge,
  sum(WallDuration) as WallDuration,
  sum(CpuUserDuration) as CpuUserDuration,
  sum(CpuSystemDuration) as CpuSystemDuration,
  NodeCount,
  count(Njobs) as Njobs,
  Processors,
  DATE(EndTime),
  MachineName,
  ProjectName
FROM
  JobUsageRecord JUR
JOIN
  JobUsageRecord_Meta JURM ON JUR.dbid = JURM.dbid
WHERE
  JUR.dbid > %%(last_successful_id)s AND
  JUR.dbid < %d + %d AND   
  ProbeName = %%(probename)s
GROUP BY
  ResourceType,
  ReportableVOName,
  LocalUserId,
  NodeCount,
  Processors,
  DATE(EndTime),
  MachineName,
  ProjectName
ORDER BY JUR.dbid ASC
LIMIT 10000
"""

# %d day of month (00-31)

def _add_if_exists(cp, attribute, info):
    """
    If section.attribute exists in the config file, add its value into a
    dictionary.

    @param cp: ConfigParser object representing our config
    @param attribute: Attribute in section.
    @param info: Dictionary that we add data into.
    """
    try:
        info[attribute] = cp.get("gratia", attribute)
    except:
        pass

def query_gratia(cp, txn=None, probename=None, starttime=None, quarantine_dbid=None):
    info = {}
    _add_if_exists(cp, "user", info)
    _add_if_exists(cp, "passwd", info)
    _add_if_exists(cp, "db", info)
    _add_if_exists(cp, "host", info)
    _add_if_exists(cp, "port", info)
    if 'port' in info:
        info['port'] = int(info['port'])
    conn = None
    try:
        conn = MySQLdb.connect(**info)
        log.debug("Successfully connected to database ...")
    except:
        raise Exception("Failed to connect to database. Following parameters were used to connect: " + str(info))
    curs = conn.cursor()

    if(probename is None):
        Range_id = None
        Ending_id = None
        #If a job was previously quarantined, we need to process records only up to that job
        if(quarantine_dbid is not None):
            log.debug("quarantine_dbid is: " + str(quarantine_dbid) + ". setting Range_id to 1") 
            Ending_id = quarantine_dbid
            Range_id = 1 
        else:
            Ending_id = txn['last_successful_id']
            Range_id = MAX_ID
            log.debug("quarantine_dbid is None. Set Ending_id to: " + str(Ending_id) + " and Range_id to: " + str(Range_id))
              

        #Define GRATIA_QUERY according to MAX_ID
        GRATIA_QUERY = QUERY % (Ending_id, Range_id)

        results = []
        curs.execute(GRATIA_QUERY, txn)
        for row in curs.fetchall():
            info = {}
            info['dbid'] = row[0] #dbid in gratia
            info['resource_type'] = row[1] # ResourceType in gratia
            info['vo_name'] = row[2] # ReportableVOName in gratia
            info['user'] = row[3] # LocalUserId in gratia
            info['charge'] = row[4] # Charge in gratia
            info['wall_duration'] = row[5] # WallDuration in gratia
            info['cpu'] = row[6] + row[7] # CpuUserDuration + CpuSystemDuration in gratia
            info['node_count'] = row[8] # NodeCount in gratia
            info['njobs'] = row[9] # Njobs in gratia
            info['processors'] = row[10] # Processors in gratia
            info['endtime'] = row[11].strftime("%Y-%m-%d %H:%M:%S") # EndTime in gratia
            info['machine_name'] = row[12] # MachineName in gratia
            info['project_name'] = row[13] # ProjectName in gratia
            info['queue'] = "condor"
            results.append(info)
        return results
    else:
        rules_minimum_dbid = 0
        query = ("select MIN(JUR.dbid) from JobUsageRecord JUR JOIN JobUsageRecord_Meta JURM ON JUR.dbid = JURM.dbid WHERE ProbeName = %s AND StartTime >= %s", [probename, starttime])
        log.debug("query is: " + str(query))
        curs.execute(*query)
        row = curs.fetchone()
        log.debug("query_gratia: curs.fetchone row is:" + str(row))
        if not None in row:
             rules_minimum_dbid = int(row[0])
        return rules_minimum_dbid

def initialize_txn(cp, probename):
    '''
    initialize the last_successful_id to be the maximum of
    the minimum dbid of the database
    and last_successful_id
    '''
    info = {}
    _add_if_exists(cp, "user", info)
    _add_if_exists(cp, "passwd", info)
    _add_if_exists(cp, "db", info)
    _add_if_exists(cp, "host", info)
    _add_if_exists(cp, "port", info)
    if 'port' in info:
        info['port'] = int(info['port'])
    db = None
    try:
        db = MySQLdb.connect(**info)
    except:
        raise Exception("Failed to connect to database. Following parameters were used to connect: " + str(info))
    cursor = db.cursor()

    #cursor.execute("select MIN(dbid), MAX(dbid) from JobUsageRecord");
    log.debug("In initialize_txn method, probename is:" + probename)
    cursor.execute("select MIN(dbid), MAX(dbid) from JobUsageRecord_Meta WHERE ProbeName = '%s'" %probename)
    row = cursor.fetchone()
    log.debug("cursor.fetchone row is:" + str(row))
    if not None in row:
         minimum_dbid = int(row[0])
         maximum_dbid = int(row[1])
    else:
         minimum_dbid = 0
         maximum_dbid = 0
 
    log.debug("minimum_dbid: " + str(minimum_dbid) + " maximum_dbid: " + str(maximum_dbid))
    # now, we want to put it into the file.
    # we check the file, if the file is empty, then it is the
    # the minimum dbid, otherwise, we choose 
    # to be the maximum of the "minimum dbid" and the last_successful_id in the file
    #txn={}
    txn_previous = transaction.start_txn(cp, probename)
    txn = txn_previous #Let txn have everything which was saved previously. We'd add/modify enty for the current probename

    #txn['last_successful_id']=max(minimum_dbid, txn_previous['last_successful_id'])
    #txn['probename'] = cp.get("gratia", "probe")

    #Check if the probename exists in txn_previous dictionary
    if probename in txn_previous:
        txn[probename] = max(minimum_dbid, txn_previous[probename])
    else:
        log.debug("probename didn't exist in dictionary from before...")
        txn[probename] = minimum_dbid
    log.debug("txn[probename] is:" + str(txn[probename]))
    transaction.commit_txn(cp, txn)
    return minimum_dbid, maximum_dbid

def summarize_gratia(cp):
    raise NotImplementedError()

