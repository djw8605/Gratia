call setup.bat
del %root%\target\Configurator.war

mkdir war
mkdir war\WEB-INF

copy %base%\GratiaReportConfiguration\WebContent\* war
copy %base%\GratiaReportConfiguration\src\web.xml war\WEB-INF

jar -cf %root%\target\gratia-report-configuration.war -C war .

rmdir /S /Q war
