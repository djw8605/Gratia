call setup.bat
mkdir %catalina_home%\gratia
copy %root%\configuration\* %catalina_home%\gratia
del %catalina_home%\gratia\local.*
del %catalina_home%\gratia\release.*
copy %root%\configuration\local.service-configuration.properties %catalina_home%\gratia\service-configuration.properties
