call setup.bat
del %root%\target\gratia-security.war

mkdir war
mkdir war\wsdl
mkdir war\meta-inf
mkdir war\WEB-INF
mkdir war\WEB-INF\classes
mkdir war\WEB-INF\classes\net
mkdir war\WEB-INF\classes\net\sf
mkdir war\WEB-INF\classes\net\sf\gratia
mkdir war\WEB-INF\classes\net\sf\gratia\services
mkdir war\WEB-INF\classes\net\sf\gratia\security

mkdir war\WEB-INF\lib

rmdir /q /s net

javac -extdirs %root%\jars %root%\services\net\sf\gratia\services\*.java
javac -extdirs %root%\jars %root%\security\net\sf\gratia\security\*.java

rmic -d . net.sf.gratia.services.JMSProxyImpl
copy net\sf\gratia\services\* %root%\services\net\sf\gratia\services

rmdir /q /s net

copy %root%\services\net\sf\gratia\services\*.class war\WEB-INF\classes\net\sf\gratia\services
copy %root%\security\net\sf\gratia\security\*.class war\WEB-INF\classes\net\sf\gratia\security

del war\WEB-INF\lib\serv*

copy %root%\security\net\sf\gratia\security\web.xml war\WEB-INF\web.xml
copy %root%\jars\mysql*.jar war\WEB-INF\lib

jar -cfM  %root%\target\gratia-security.war -C war .

rmdir /q /s war

#
# remove later
#

rmdir /q /s \tomcat\webapps\gratia-security
copy %root%\target\gratia-security.war \tomcat\webapps