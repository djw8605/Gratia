sendtohtml ./range "$ExtraArgs $whenarg" ${WORK_DIR}/report "$MAIL_MSG" $RECIPIENT
sendtohtml ./reporting "$ExtraArgs $whenarg" ${WORK_DIR}/report "$REPORTING_MAIL_MSG" $RECIPIENT
sendtohtml ./longjobs "$ExtraArgs $whenarg" ${WORK_DIR}/report "$LONGJOBS_MAIL_MSG" $RECIPIENT
sendtohtml ./usersreport "$ExtraArgs $whenarg" ${WORK_DIR}/report "$USER_MAIL_MSG" $RECIPIENT
sendtohtml ./efficiency "$ExtraArgs $whenarg" ${WORK_DIR}/report "${ExtraHeader}OSG Efficiency by Site and VO for $when" $RECIPIENT
sendtohtml ./voefficiency "$ExtraArgs $whenarg" ${WORK_DIR}/report "${ExtraHeader}OSG Efficiency by VO for $when" $RECIPIENT
sendtohtml ./gradedefficiency "$ExtraArgs $whenarg" ${WORK_DIR}/report "${ExtraHeader}OSG Efficiency by VO by time period for $when" $RECIPIENT
sendtohtml ./transfer "$ExtraArgs $whenarg" ${WORK_DIR}/report "$TR_MAIL_MSG" $RECIPIENT
sendtohtml ./usersitereport "$ExtraArgs $whenarg" ${WORK_DIR}/report "${ExtraHeader}Report by user by site for $when" $USER_RECIPIENT
sendtohtml ./compareVOs.py "$ExtraArgs $whenarg" ${WORK_DIR}/report "Subject will be set in compareVOs.py" $RECIPIENT 
if [ "$ExtraArgs" == "--monthly" ] ; then
  sendtohtml ./softwareVersions "$ExtraArgs $whenarg" ${WORK_DIR}/report "OSG Installed Probe Versions as of $when" $RECIPIENT 
fi
