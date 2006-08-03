call setup.bat

rmdir /S /Q tarball
mkdir tarball
del *.tar

#
# create condor tarball
#
mkdir tarball\gratia
mkdir tarball\gratia\gratia_probes
mkdir tarball\var

mkdir tarball\var\data
echo xxx > tarball\var\data\ignoreme
mkdir tarball\var\logs
echo xxx > tarball\var\logs\ignoreme
mkdir tarball\var\tmp
echo xxx > tarball\var\tmp\ignoreme

copy %root%\condor-probe\* tarball\gratia\gratia_probes
cd tarball
tar -cvf ..\gratia_probe_v0.2.tar ./gratia ./var
cd ..
rmdir /S /Q tarball

#
# create reporting tarball
#
mkdir tarball
mkdir tarball\gratia
mkdir tarball\gratia\gratia_reporting
mkdir tarball\var
mkdir tarball\var\data
echo xxx > tarball\var\data\ignoreme
mkdir tarball\var\logs
echo xxx > tarball\var\logs\ignoreme
mkdir tarball\var\tmp
echo xxx > tarball\var\tmp\ignoreme

copy %root%\target\GratiaReportConfiguration.war tarball\gratia\gratia_reporting
copy %root%\target\GratiaReports.war tarball\gratia\gratia_reporting
copy %root%\target\GratiaReporting.war tarball\gratia\gratia_reporting
cd tarball
tar -cvf ..\gratia_reporting_v0.1.tar ./gratia ./var
cd ..
rmdir /S /Q tarball

#
# create services tarball
#
mkdir tarball
mkdir tarball\tomcat
mkdir tarball\tomcat\v55
mkdir tarball\tomcat\v55\gratia
mkdir tarball\gratia
mkdir tarball\gratia\gratia_services
mkdir tarball\var
mkdir tarball\var\data
echo xxx > tarball\var\data\ignoreme
mkdir tarball\var\logs
echo xxx > tarball\var\logs\ignoreme
mkdir tarball\var\tmp
echo xxx > tarball\var\tmp\ignoreme

copy %root%\configuration\* tarball\tomcat\v55\gratia
del tarball\tomcat\v55\gratia\local.*
del tarball\tomcat\v55\gratia\psg3.*
del tarball\tomcat\v55\gratia\release.*
copy %root%\configuration\release.service-configuration.properties tarball\tomcat\v55\gratia\service-configuration.properties

copy %root%\target\gratia-services.war tarball\gratia\gratia_services
copy %root%\target\gratia-servlets.war tarball\gratia\gratia_services
copy %root%\target\gratia-security.war tarball\gratia\gratia_services
copy %root%\target\GratiaServices.war tarball\gratia\gratia_services

cd tarball
tar -cvf ..\gratia_services_v0.2.tar ./gratia ./tomcat ./var
cd ..
rmdir /S /Q tarball

#
# finally - clean everything up
#
copy *.tar %root%\target
del *.tar

