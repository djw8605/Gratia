call setup.bat
del %root%\target\gratia-servlets.war

mkdir war
mkdir war\wsdl
mkdir war\meta-inf
mkdir war\WEB-INF
mkdir war\WEB-INF\classes
mkdir war\WEB-INF\classes\net
mkdir war\WEB-INF\classes\net\sf
mkdir war\WEB-INF\classes\net\sf\gratia
mkdir war\WEB-INF\classes\net\sf\gratia\services
mkdir war\WEB-INF\classes\net\sf\gratia\servlets

mkdir war\WEB-INF\lib

rmdir /q /s net

javac -extdirs %root%\jars %root%\services\net\sf\gratia\services\*.java
javac -extdirs %root%\jars %root%\servlets\net\sf\gratia\servlets\*.java

rmic -d . net.sf.gratia.services.JMSProxyImpl
copy net\sf\gratia\services\* %root%\services\net\sf\gratia\services

rmdir /q /s net

copy %root%\services\net\sf\gratia\services\Logging.class war\WEB-INF\classes\net\sf\gratia\services
copy %root%\services\net\sf\gratia\services\Configuration.class war\WEB-INF\classes\net\sf\gratia\services
copy %root%\services\net\sf\gratia\services\*Skel.class war\WEB-INF\classes\net\sf\gratia\services
copy %root%\services\net\sf\gratia\services\*Stub.class war\WEB-INF\classes\net\sf\gratia\services
copy %root%\services\net\sf\gratia\services\X*.class war\WEB-INF\classes\net\sf\gratia\services
copy %root%\services\net\sf\gratia\services\JMS*.class war\WEB-INF\classes\net\sf\gratia\services

copy %root%\servlets\net\sf\gratia\servlets\RMIHandler*.class war\WEB-INF\classes\net\sf\gratia\servlets

del war\WEB-INF\lib\serv*

copy %root%\servlets\net\sf\gratia\servlets\web.xml war\WEB-INF\web.xml

jar -cfM  %root%\target\gratia-servlets.war -C war .

rmdir /q /s war

#
# remove later
#

rmdir /q /s \tomcat\webapps\gratia-servlets
copy %root%\target\gratia-servlets.war \tomcat\webapps
