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
del %catalina_home%\gratia\local.*
del %catalina_home%\gratia\psg3.*
del %catalina_home%\gratia\release.*
copy %root%\configuration\local.service-configuration.properties %catalina_home%\gratia\service-configuration.properties
copy %root%\wars\* %root%\target
copy %root%\target\* %catalina_home%\webapps
