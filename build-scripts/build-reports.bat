call setup.bat

del %root%\target\GratiaReports.war

mkdir war
mkdir war\images
mkdir war\WEB-INF

copy %base%\GratiaReports\WebContent\* war
copy %base%\GratiaReports\WebContent\images\* war\images
copy %base%\GratiaReports\src\web.xml war\WEB-INF

jar -cf %root%\target\gratia-reports.war -C war .

rmdir /q /s war
