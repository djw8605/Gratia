call setup.bat
del %root%\target\GratiaServices.war

mkdir war
mkdir war\meta-inf
mkdir war\WEB-INF
mkdir war\WEB-INF\classes
mkdir war\WEB-INF\classes\net
mkdir war\WEB-INF\classes\net\sf
mkdir war\WEB-INF\classes\net\sf\gratia
mkdir war\WEB-INF\classes\net\sf\gratia\soap
mkdir war\WEB-INF\classes\net\sf\gratia\services
mkdir war\WEB-INF\lib

mkdir war\wsdl

javac -extdirs %root%\jars %root%\services\net\sf\gratia\services\*.java
javac -extdirs %root%\jars %root%\soap\net\sf\gratia\soap\*.java
rmic -d . net.sf.gratia.services.JMSProxyImpl

copy net\sf\gratia\services\* %root%\services\net\sf\gratia\services

rmdir /q /s net

copy %root%\services\net\sf\gratia\services\Logging.class war\WEB-INF\classes\net\sf\gratia\services
copy %root%\services\net\sf\gratia\services\Configuration.class war\WEB-INF\classes\net\sf\gratia\services
copy %root%\services\net\sf\gratia\services\*Skel.class war\WEB-INF\classes\net\sf\gratia\services
copy %root%\services\net\sf\gratia\services\*Stub.class war\WEB-INF\classes\net\sf\gratia\services
copy %root%\services\net\sf\gratia\services\X*.class war\WEB-INF\classes\net\sf\gratia\services
copy %root%\services\net\sf\gratia\services\JMS*.class war\WEB-INF\classes\net\sf\gratia\services

copy %root%\soap\net\sf\gratia\soap\* war\WEB-INF\classes\net\sf\gratia\soap

copy %root%\soap\net\sf\gratia\soap\server-config.wsdd war\WEB-INF\server-config.wsdd
copy %root%\soap\net\sf\gratia\soap\web.xml war\WEB-INF\web.xml
copy %root%\soap\net\sf\gratia\soap\collector.wsdl war\wsdl

copy %root%\jars\axis*.jar war\WEB-INF\lib
copy %root%\jars\commons-dis*.jar war\WEB-INF\lib
copy %root%\jars\jaxrpc*.jar war\WEB-INF\lib
copy %root%\jars\saaj*.jar war\WEB-INF\lib
copy %root%\jars\wsdl*.jar war\WEB-INF\lib

jar -cfM %root%\target\GratiaServices.war -C war .

rmdir /S /Q war
