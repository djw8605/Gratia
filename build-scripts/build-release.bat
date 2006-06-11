call setup.bat
call cleanup.bat
call build-services.bat
call build-soap.bat
call build-servlets.bat
call build-reporting.bat
call build-reports.bat
call build-configurator.bat
copy %root%\configuration\* %root%\target
copy %root%\configuration-release\* %root%\target
copy %root%\wars\* %root%\target

