call setup.bat

rmdir /S /Q tarball
mkdir tarball
del *.tar

#
# create condor tarball
#
mkdir tarball\gratia
mkdir tarball\gratia\gratia_probes
copy %root%\condor-probe\* tarball\gratia\gratia_probes
cd tarball
tar -cvf ..\gratia_probe_v0.2.tar ./gratia
cd ..
rmdir /S /Q tarball

#
# create reporting tarball
#
mkdir tarball
mkdir tarball\gratia
mkdir tarball\gratia\gratia_reporting
copy %root%\target\Configurator.war tarball\gratia\gratia_reporting
copy %root%\target\reports.war tarball\gratia\gratia_reporting
copy %root%\target\GratiaReporting.war tarball\gratia\gratia_reporting
cd tarball
tar -cvf ..\gratia_reporting_v0.1.tar ./gratia
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

copy %root%\configuration\* tarball\tomcat\v55\gratia
copy %root%\configuration-release\* tarball\tomcat\v55\gratia

copy %root%\target\gratia-services.war tarball\gratia\gratia_services
copy %root%\target\gratia-servlets.war tarball\gratia\gratia_services
copy %root%\target\GratiaServices.war tarball\gratia\gratia_services

cd tarball
tar -cvf ..\gratia_services_v0.2.tar ./gratia ./tomcat
cd ..
rmdir /S /Q tarball

#
# finally - clean everything up
#
copy *.tar %root%\target
del *.tar

