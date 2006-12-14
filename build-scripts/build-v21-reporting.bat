call setup.bat
del %root%\target\gratia-v21-reporting.war

mkdir war
mkdir war\calendar
mkdir war\images
mkdir war\WEB-INF
mkdir war\WEB-INF\classes
mkdir war\WEB-INF\classes\net
mkdir war\WEB-INF\classes\net\sf
mkdir war\WEB-INF\classes\net\sf\gratia
mkdir war\WEB-INF\classes\net\sf\gratia\services
mkdir war\WEB-INF\classes\net\sf\gratia\reporting
mkdir war\WEB-INF\classes\net\sf\gratia\reporting\exceptions

mkdir war\WEB-INF\lib

javac -extdirs %root%\jars %root%\services\net\sf\gratia\services\*.java
javac -extdirs %root%\jars %root%\gratia-v21-reporting\src\net\sf\gratia\reporting\*.java
javac -extdirs %root%\jars %root%\gratia-v21-reporting\src\net\sf\gratia\reporting\exceptions\*.java

copy %root%\reporting\net\sf\gratia\reporting\*.jsp war
copy %root%\reporting\net\sf\gratia\reporting\*.css war

copy %root%\services\net\sf\gratia\services\*.class war\WEB-INF\classes\net\sf\gratia\services
copy %root%\gratia-v21-reporting\src\net\sf\gratia\reporting\*.class war\WEB-INF\classes\net\sf\gratia\reporting
copy %root%\gratia-v21-reporting\src\net\sf\gratia\reporting\exceptions\*.class war\WEB-INF\classes\net\sf\gratia\reporting\exceptions
xcopy /s %root%\gratia-v21-reporting\WebContent\calendar war\calendar
xcopy /s %root%\gratia-v21-reporting\WebContent\images\* war\images
copy %root%\gratia-v21-reporting\WebContent\* war

copy %root%\gratia-v21-reporting\src\net\sf\gratia\reporting\web.xml war\WEB-INF
copy %root%\gratia-v21-reporting\jars\* war\WEB-INF\lib
del war\WEB-INF\lib\servlet-api.jar

jar -cf  %root%\target\gratia-v21-reporting.war -C war .

rmdir /q /s war

#
# remove later
#

rmdir /q /s \tomcat\webapps\gratia-v21-reporting
copy %root%\target\gratia-v21-reporting.war \tomcat\webapps
