
"""
Module for managing in-progress transactions.

Allows us to rollback Gratia->Gold uploads on failure.
"""

import os
import md5
import logging

import simplejson

import gold
import sys
import stat
import errno

log = logging.getLogger("gratia_gold.transaction")

def check_rollback(cp):
    """
    Read the rollback log, and rollback any pending charges.
    """
    try:
        rollback_file = cp.get("transaction", "rollback")
        if not os.path.exists(rollback_file) or not os.access(rollback_file,
                os.R_OK):
            log.debug("returning new rollback_file")
            return open(rollback_file, "w")
        else:
            r_file = open(rollback_file, 'r')
            contents = r_file.read()
            log.debug("rollback_file contents: " + str(contents))
            r_file.close()
    except Exception, e:
        log.exception("Caught an exception and the detail is: \n\"" + str(e) + "\"!")
        
    refund_file = "%s.refund" % rollback_file
    try:
        refund_fd = open(refund_file, "r+")
    except IOError, ie:
        if ie.errno != 2:
            log.exception("Caught an exception when opening refund_file.")
            raise Exception("Error code is: " + str(ie.errno))
        refund_fd = None
    refund_count = 0
    try:
        if refund_fd:
            for line in refund_fd.readlines():
                refund_count += 1
                log.debug("refund line is: " + str(line))
            log.info("There are %i refunds" % refund_count)
        else:
            refund_fd = open(refund_file, "w")
            log.debug("Opened a new refund_file since none existed before.")
    except Exception, e:
        log.exception("Caught an exception when reading refund_fd")

    # Check for a rollback file - if there is none, there's no transaction to
    # undo; just return a new file handle.
    try:
        rollback_fd = open(rollback_file, "r")
    except IOError, ie:
        if ie.errno == 2:
            return open(rollback_file, "w")
        raise Exception("Error code is: " + str(ie.errno))
    skip_count = 0
    # We have a rollback file.  If there's refunds already issued, skip those.
    for line in rollback_fd.readlines():
        log.debug("rollback file line is: " + str(line))
        skip_count += 1
        if skip_count <= refund_count:
            continue
        # Parse the rollback to prepare the refund
        md5sum, job = line.strip().split(":",1)
        md5sum2 = md5.md5(job).hexdigest()
        if md5sum != md5sum2:
            raise Exception("Rollback log doesn't match md5sum (%s!=%s): %s" \
                % (md5sum, md5sum2, line.strip()))
        job_dict = simplejson.loads(job)
        log.debug("job_dict is: " + str(job_dict))
        # Perform refund, then write it out.  We err on the side of issuing
        # too many refunds.
        try:
            gold.refund(cp, job_dict)
        except:
            log.exception("gold.refund method failed !")
            raise Exception("gold.refund method failed !")
        refund_fd.write(line)
        os.fsync(refund_fd.fileno())
    rollback_fd.close()
    refund_fd.close()
    # We were able to rollback everything that failed - remove the records
    os.unlink(rollback_file)
    os.unlink(refund_file)
    return open(rollback_file, "w")

def add_rollback(fd, job):
    job_str = simplejson.dumps(job)
    if len(job_str.split("\n")) > 1:
        raise Exception("Job description contains newline")
    digest = md5.md5(job_str).hexdigest()
    fd.write("%s:%s\n" % (digest, job_str))
    os.fsync(fd.fileno())


def start_txn(cp, probename):
    '''
    read the content of the txn file
    '''
    txn_file = cp.get("transaction", "last_successful_id")
    try:
        txn_fp = open(txn_file, "r")
        txn_obj = simplejson.load(txn_fp)
        txn_fp.close()
        return txn_obj
    except IOError, ie:
        log.debug("*****start_txn, returning 0 value for: " + probename + "*****")
        return {probename: 0}
    except:
        exception_type = sys.exc_info()[0]
        log.error("Unexpected error: " + str(exception_type))
        raise

def commit_txn(cp, txn):
    '''
    update the txn file
    '''
    txn_file = cp.get("transaction", "last_successful_id")
    try:
        txn_fp = open(txn_file, "w")
    except:
        raise Exception("Unable to open: " + str(txn_file) + " for writing")
    simplejson.dump(txn, txn_fp)
    log.debug("Updating ... " + str(txn))
    os.fsync(txn_fp.fileno())
    txn_fp.close()
    rollback_file = cp.get("transaction", "rollback")
    try:
        os.unlink(rollback_file)
    except:
        pass
    refund_file = "%s.refund" % rollback_file
    try:
        os.unlink(refund_file)
    except:
        pass

def get_qt(cp, previous_query = None):
    '''
    get the object which contains the quarantined transactions - return a non-zero return value for any exception
    '''
    q_dir = cp.get("quarantine", "quarantine_directory")
    if(previous_query is None):
        q_file = q_dir + '/qt_id'
    else:
        q_file = q_dir + '/pt_id'
    try:
        q_fp = open(q_file, "r")
        q_obj = simplejson.load(q_fp)
        q_fp.close()
        return q_obj
    except:
        raise

def commit_qt(cp, qt, previous_query = None):
    '''
    update the qt file
    '''
    q_dir = cp.get("quarantine", "quarantine_directory")
    if(previous_query is None):
        q_file = q_dir + '/qt_id'
    else:
        q_file = q_dir + '/pt_id'
    try:
        q_fp = open(q_file, "w")
        simplejson.dump(qt, q_fp)
        log.debug("Updating ... " + str(qt))
        os.fsync(q_fp.fileno())
        q_fp.close()
    except:
        raise Exception("Unable to open: " + str(q_file) + " for writing")

def delete_qt(cp):
    '''
    delete the qt file
    '''
    qt_dir = cp.get("quarantine", "quarantine_directory")
    #Need to delete the file named "qt_id" in the quarantine_directory
    qt_file = qt_dir + '/qt_id'
    pt_file = qt_dir + '/pt_id'
    try:
        os.remove(qt_file)
        os.remove(pt_file)
        log.debug("deleted " + str(qt_file))
        log.debug("deleted " + str(pt_file))
    except:
        log.error("*****Unable to delete " + str(qt_file) + " and " + str(pt_file) + ". This will cause issues with the gratia-gold software*****")

def get_quarantined_job_dbid(cp, probename):
    '''
    Given the probename, return the dbid of the quarantined job - return a zero db_id value for any exception
    '''
    qt_dir = cp.get("quarantine", "quarantine_directory")
    qt_file = qt_dir + '/qt_id'
    try:
        qt_fp = open(qt_file, "r")
        qt_obj = simplejson.load(qt_fp)
        qt_fp.close()
        if(probename in qt_obj):
            log.debug(str(probename) + " found in the quarantine dictionary.")
            return qt_obj
        else:
            log.debug(str(probename) + " was NOT found in the quarantine dictionary.")
            return {}
    except IOError, e:
        if e.errno != errno.ENOENT:
            log.warning("The quarantine file exists but could not be opened - something went wrong here...")
            raise
        else: 
            log.debug("Unable to open quarantine database " + str(qt_file) + " since it does not exist, yet.")
        return {}
    except:
        exception_type = sys.exc_info()[0]
        log.error("Unexpected error: " + str(exception_type))
        raise
