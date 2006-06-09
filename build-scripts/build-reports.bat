call setup.bat

del %root%\target\reports.war

mkdir war
mkdir war\images
mkdir war\WEB-INF

copy %root%\reports\* war
copy %root%\reports\images\* war\images
copy %root%\reports\web.xml war\WEB-INF

jar -cf %root%\target\reports.war -C war .

rmdir /q /s war
