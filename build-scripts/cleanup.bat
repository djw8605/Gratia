call setup.bat

del %root%\services\net\sf\gratia\services\*~
del %root%\services\net\sf\gratia\services\*#
del %root%\services\net\sf\gratia\services\*.class

del %root%\services\net\sf\gratia\storage\*~
del %root%\services\net\sf\gratia\storage\*#
del %root%\services\net\sf\gratia\storage\*.class

del %root%\soap\net\sf\gratia\soap\*~
del %root%\soap\net\sf\gratia\soap\*#
del %root%\soap\net\sf\gratia\soap\*.class

del %root%\servlets\net\sf\gratia\servlets\*~
del %root%\servlets\net\sf\gratia\servlets\*#
del %root%\servlets\net\sf\gratia\servlets\*.class

del %base%GratiaReporting\src\net\sf\gratia\reporting\*~
del %base%GratiaReporting\src\net\sf\gratia\reporting\*#
del %base%GratiaReporting\src\net\sf\gratia\reporting\*.class

del %base%GratiaReporting\src\net\sf\gratia\reporting\exceptions\*~
del %base%GratiaReporting\src\net\sf\gratia\reporting\exceptions\*#
del %base%GratiaReporting\src\net\sf\gratia\reporting\exceptions\*.class

del %root%\configuration\*~
del %root%\configuration\*#

del %root%\configuration-local\*~
del %root%\configuration-local\*#

del %root%\configuration-psg3\*~
del %root%\configuration-psg3\*#

del %root%\configuration-release\*~
del %root%\configuration-release\*#

del /q %root%\target\*
mkdir %root%\target

del *~
del *#
del *.class
