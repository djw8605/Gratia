call setup.bat
call cleanup.bat
call build-tomcat.bat
call build-services.bat
call build-soap.bat
call build-servlets.bat
call build-reporting.bat
call build-reports.bat
call build-configurator.bat
copy %root%\configuration-psg3\* \tomcat\gratia
copy %root%\wars\* %root%\target
copy %root%\target\* \tomcat\webapps
