WEBLOC="http://gratia-fermi-transfer.fnal.gov/gratia-reporting/"
sendto ./reporting "$ExtraArgs $whenarg" ${WORK_DIR}/report "$REPORTING_MAIL_MSG" $MAILTO
sendto ./usersreport "$ExtraArgs $whenarg" ${WORK_DIR}/report "$USER_MAIL_MSG" $MAILTO
sendto ./transfer "$ExtraArgs $whenarg" ${WORK_DIR}/report "$TR_MAIL_MSG" $MAILTO
