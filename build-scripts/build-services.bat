call setup.bat

del %root%\target\gratia-services.war

mkdir war
mkdir war\meta-inf
mkdir war\WEB-INF
mkdir war\WEB-INF\classes
mkdir war\WEB-INF\classes\net
mkdir war\WEB-INF\classes\net\sf
mkdir war\WEB-INF\classes\net\sf\gratia
mkdir war\WEB-INF\classes\net\sf\gratia\services
mkdir war\WEB-INF\classes\net\sf\gratia\storage
mkdir war\WEB-INF\lib

rmdir /q /s net

javac -extdirs %root%\jars %root%\services\net\sf\gratia\services\*.java
javac -extdirs %root%\jars %root%\storage\net\sf\gratia\storage\*.java

rmic -d . net.sf.gratia.services.JMSProxyImpl
copy net\sf\gratia\services\* %root%\services\net\sf\gratia\services

rmdir /q /s net

copy %root%\services%\net\sf\gratia\services\*.class war\WEB-INF\classes\net\sf\gratia\services
copy %root%\storage\net\sf\gratia\storage\*.class war\WEB-INF\classes\net\sf\gratia\storage

copy %root%\jars\* war\WEB-INF\lib
del war\WEB-INF\lib\serv*

copy %root%\services\net\sf\gratia\services\web.xml war\WEB-INF\web.xml

jar -cfM %root%\target\gratia-services.war -C war .

rmdir /q /s war
