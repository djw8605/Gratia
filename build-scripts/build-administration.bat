call setup.bat
del %root%\target\gratia-administration.war

#
# build cetable administration
#

mkdir war
mkdir war\images
mkdir war\wsdl
mkdir war\meta-inf
mkdir war\WEB-INF
mkdir war\WEB-INF\classes
mkdir war\WEB-INF\classes\net
mkdir war\WEB-INF\classes\net\sf
mkdir war\WEB-INF\classes\net\sf\gratia
mkdir war\WEB-INF\classes\net\sf\gratia\services
mkdir war\WEB-INF\classes\net\sf\gratia\administration

mkdir war\WEB-INF\lib

rmdir /q /s net

javac -extdirs %root%\jars %root%\services\net\sf\gratia\services\*.java
javac -extdirs %root%\jars %root%\administration\net\sf\gratia\administration\*.java

rmic -d . net.sf.gratia.services.JMSProxyImpl
copy net\sf\gratia\services\* %root%\services\net\sf\gratia\services

rmdir /q /s net

copy %root%\services\net\sf\gratia\services\*.class war\WEB-INF\classes\net\sf\gratia\services
copy %root%\administration\net\sf\gratia\administration\*.class war\WEB-INF\classes\net\sf\gratia\administration
copy %root%\administration\net\sf\gratia\administration\*.html war
copy %root%\*.html war
copy %root%\administration\net\sf\gratia\administration\*.jsp war
copy %root%\administration\net\sf\gratia\administration\images\*.gif war\images

copy %root%\administration\net\sf\gratia\administration\web.xml war\WEB-INF\web.xml
copy %root%\jars\mysql*.jar war\WEB-INF\lib

jar -cfM  %root%\target\gratia-administration.war -C war .

rmdir /q /s war

#
# remove later
#

rmdir /q /s \tomcat\webapps\gratia-administration
copy %root%\target\gratia-administration.war \tomcat\webapps
