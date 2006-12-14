call setup.bat

del %root%\target\gratia-v21-reports.war

mkdir war
mkdir war\images
mkdir war\WEB-INF

copy %root%\gratia-v21-reports\* war
copy %root%\gratia-v21-reports\images\* war\images
copy %root%\gratia-v21-reports\web.xml war\WEB-INF

jar -cf %root%\target\gratia-v21-reports.war -C war .

rmdir /q /s war

#
# remove
#
rmdir /q /s \tomcat\webapps\gratia-v21-reports
copy %root%\target\gratia-v21-reports.war \tomcat\webapps
