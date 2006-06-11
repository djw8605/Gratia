call setup.bat
call cleanup.bat
call build-tomcat.bat
call build-services.bat
call build-soap.bat
call build-servlets.bat
call build-reporting.bat
call build-reports.bat
call build-configurator.bat
mkdir %catalina_home%\gratia
copy %root%\configuration\* %catalina_home%\gratia
copy %root%\configuration-local\* %catalina_home%\gratia
copy %root%\wars\* %root%\target
copy %root%\target\* %catalina_home%\webapps
