call setup.bat
del %root%\target\Configurator.war

mkdir war
mkdir war\WEB-INF

copy %root%\configurator\* war
copy %root%\configurator\web.xml war\WEB-INF

jar -cf %root%\target\Configurator.war -C war .

rmdir /S /Q war
