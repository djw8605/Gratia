call setup.bat

del %root%\target\gratia-summary-reports.war

mkdir war
mkdir war\images
mkdir war\WEB-INF

copy %root%\summary-reports\* war
copy %root%\summary-reports\images\* war\images
copy %root%\summary-reports\web.xml war\WEB-INF

jar -cf %root%\target\gratia-summary-reports.war -C war .

rmdir /q /s war

#
# remove
#
rmdir /q /s \tomcat\webapps\gratia-summary-reports
copy %root%\target\gratia-summary-reports.war \tomcat\webapps
