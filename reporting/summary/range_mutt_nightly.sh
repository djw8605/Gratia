sendtohtml $INSTALL_DIR/range "$ExtraArgs $whenarg" ${WORK_DIR}/report "$MAIL_MSG" $RECIPIENT
sendtohtml $INSTALL_DIR/reporting "$ExtraArgs $whenarg" ${WORK_DIR}/report "$REPORTING_MAIL_MSG" $RECIPIENT
sendtohtml $INSTALL_DIR/longjobs "$ExtraArgs $whenarg" ${WORK_DIR}/report "$LONGJOBS_MAIL_MSG" $RECIPIENT
sendtohtml $INSTALL_DIR/usersreport "$ExtraArgs $whenarg" ${WORK_DIR}/report "$USER_MAIL_MSG" $RECIPIENT
sendtohtml $INSTALL_DIR/efficiency "$ExtraArgs $whenarg" ${WORK_DIR}/report "${ExtraHeader}OSG Efficiency by Site and VO for $when" $RECIPIENT
sendtohtml $INSTALL_DIR/voefficiency "$ExtraArgs $whenarg" ${WORK_DIR}/report "${ExtraHeader}OSG Efficiency by VO for $when" $RECIPIENT
sendtohtml $INSTALL_DIR/gradedefficiency "$ExtraArgs $whenarg" ${WORK_DIR}/report "${ExtraHeader}OSG Efficiency by VO by time period for $when" $RECIPIENT
sendtohtml $INSTALL_DIR/transfer "$ExtraArgs $whenarg" ${WORK_DIR}/report "$TR_MAIL_MSG" $RECIPIENT
sendtohtml $INSTALL_DIR/usersitereport "$ExtraArgs $whenarg" ${WORK_DIR}/report "${ExtraHeader}Report by user by site for $when" $USER_RECIPIENT
sendtohtml $INSTALL_DIR/compareVOs.py "$ExtraArgs $whenarg" ${WORK_DIR}/report "Subject will be set in compareVOs.py" $RECIPIENT 
if [ "$ExtraArgs" == "--monthly" ] ; then
  sendtohtml $INSTALL_DIR/softwareVersions "$ExtraArgs $whenarg" ${WORK_DIR}/report "OSG Installed Probe Versions as of $when" $RECIPIENT 
fi
