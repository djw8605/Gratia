call setup.bat
call cleanup.bat
call build-services.bat
call build-soap.bat
call build-servlets.bat
call build-reporting.bat
call build-reports.bat
call build-configurator.bat
copy %root%\configuration\* %root%\target
del %root%\target\local.*
del %root%\target\psg3.*
del %root%\target\release.*
copy %root%\configuration\psg3.service-configuration.properties %root%\target\service-configuration.properties
copy %root%\wars\* %root%\target

