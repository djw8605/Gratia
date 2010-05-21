sendtohtml  ./range "$ExtraArgs $whenarg" ${WORK_DIR}/report "$MAIL_MSG" $MAILTO
sendtohtml ./reporting "$ExtraArgs $whenarg" ${WORK_DIR}/report "$REPORTING_MAIL_MSG" $MAILTO
sendtohtml ./longjobs "$ExtraArgs $whenarg" ${WORK_DIR}/report "$LONGJOBS_MAIL_MSG" $MAILTO
sendtohtml ./usersreport "$ExtraArgs $whenarg" ${WORK_DIR}/report "$USER_MAIL_MSG" $MAILTO
sendtohtml ./efficiency "$ExtraArgs $whenarg" ${WORK_DIR}/report "${ExtraHeader}OSG Efficiency by Site and VO for $when" $MAILTO
sendtohtml ./voefficiency "$ExtraArgs $whenarg" ${WORK_DIR}/report "${ExtraHeader}OSG Efficiency by VO for $when" $MAILTO
sendtohtml ./gradedefficiency "$ExtraArgs $whenarg" ${WORK_DIR}/report "${ExtraHeader}OSG Efficiency by VO by time period for $when" $MAILTO
sendtohtml ./transfer "$ExtraArgs $whenarg" ${WORK_DIR}/report "$TR_MAIL_MSG" $MAILTO
sendtohtml ./usersitereport "$ExtraArgs $whenarg" ${WORK_DIR}/report "${ExtraHeader}Report by user by site for $when" $USER_MAILTO
sendtohtml ./compareVOs.py "$ExtraArgs $whenarg" ${WORK_DIR}/report "Subject will be set in compareVOs.py" $MAILTO 
if [ "$ExtraArgs" == "--monthly" ] ; then
  sendtohtml ./softwareVersions "$ExtraArgs $whenarg" ${WORK_DIR}/report "OSG Installed Probe Versions as of $when" $MAILTO 
fi
