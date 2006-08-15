call setup.bat
del %root%\target\GratiaReporting.war

mkdir war
mkdir war\calendar
mkdir war\images
mkdir war\WEB-INF
mkdir war\WEB-INF\classes
mkdir war\WEB-INF\classes\net
mkdir war\WEB-INF\classes\net\sf
mkdir war\WEB-INF\classes\net\sf\gratia
mkdir war\WEB-INF\classes\net\sf\gratia\reporting
mkdir war\WEB-INF\classes\net\sf\gratia\reporting\exceptions

mkdir war\WEB-INF\lib

javac -extdirs %root%\jars %base%\GratiaReporting\src\net\sf\gratia\reporting\*.java
javac -extdirs %root%\jars %base%\GratiaReporting\src\net\sf\gratia\reporting\exceptions\*.java

copy %root%\reporting\net\sf\gratia\reporting\*.jsp war
copy %root%\reporting\net\sf\gratia\reporting\*.css war

copy %base%\GratiaReporting\src\net\sf\gratia\reporting\*.class war\WEB-INF\classes\net\sf\gratia\reporting
copy %base%\GratiaReporting\src\net\sf\gratia\reporting\exceptions\*.class war\WEB-INF\classes\net\sf\gratia\reporting\exceptions
xcopy /s %base%\GratiaReporting\WebContent\calendar war\calendar
xcopy /s %base%\GratiaReporting\WebContent\images\* war\images
copy %base%\GratiaReporting\WebContent\* war

copy %base%\GratiaReporting\src\net\sf\gratia\reporting\web.xml war\WEB-INF
copy %root%\jars\* war\WEB-INF\lib
del war\WEB-INF\lib\servlet-api.jar

jar -cf  %root%\target\gratia-reporting.war -C war .

rmdir /q /s war

#
# remove later
#

rmdir /q /s \tomcat\webapps\gratia-reporting
copy %root%\target\gratia-reporting.war \tomcat\webapps