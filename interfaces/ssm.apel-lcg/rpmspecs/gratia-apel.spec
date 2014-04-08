# Determine python location
%if ! (0%{?fedora} > 12 || 0%{?rhel} > 5)
%{!?python_sitelib: %global python_sitelib %(%{__python} -c "from distutils.sysconfig import get_python_lib; print(get_python_lib())")}
%{!?python_sitearch: %global python_sitearch %(%{__python} -c "from distutils.sysconfig import get_python_lib; print(get_python_lib(1))")}
%endif

Name:     gratia-apel
Version:  1.00
Release:  3%{?dist}

Summary:  Gratia/APEL Interface
Group:    Applications/System
License:  GPL
Group:    Applications/System
URL:      http://sourceforge.net/projects/gratia/interfaces/ssm.apel-lcg
Vendor:   The Open Science Grid <http://www.opensciencegrid.org/>

# Created by:
# svn export https://gratia.svn.sourceforge.net/svnroot/gratia/branches/dev/v1_10_rpm gratia-1.11
# tar zcf gratia-1.11.tar.gz gratia-1.11
Source0: %{name}.tar.gz

# Build settings 
BuildRoot: %{_tmppath}/%{name}-%{version}-%{release}-buildroot-%(%{__id_u} -n)
BuildArch: noarch

# Dependencies required by  the APEL-SSM code 
Requires(pre): shadow-utils
Requires: python-daemon
Requires: python-ldap
Requires: stomppy
Requires: yum-priorities
Requires: m2crypto
Requires: osg-ca-certs
%if 0%{?rhel} < 6
Requires: fetch-crl3
%endif
%if 0%{?rhel} == 6
Requires: fetch-crl
%endif

# Dependencies required by the Gratia interface code
Requires: osg-version
Requires: mysql
Requires: httpd

%description
Sends Gratia accounting data to the APEL/EGI accounting system for
Tier 1 and Tier 2 OSG resource groups using the APEL-SSM protocal.  
The SSM is designed to give a reliable message transfer mechanism
using the STOMP protocol.  Messages are encrypted during transit,
and are sent sequentially, the next message being sent only when
the previous one has been acknowledged.

This RPM is intended for SL5 only.

%prep
echo "... in prep"
%setup -q -n %{name}

##%build
##echo "... in build"

# pre processing
%pre
echo "... in pre"


### Install ####
%install
rm -rf %{buildroot}
mkdir  %{buildroot}
#---- Gratia files ------
%global source             %{_builddir}/%{name}
%global ssm_source         %{_builddir}/%{name}/ssm-1.2-1
## %global gratia_python     %{python_sitelib}/%{name}
%global gratia_logs        %{_localstatedir}/log/%{name}
%global gratia_varlib      %{_localstatedir}/lib/%{name}
%global gratia_lockfile    %{_localstatedir}/lock/subsys/gratia-apel-cron
%global ssm_rundir         %{_localstatedir}/run
%global httpd              %{_localstatedir}/www/html
%global gratia_sysconfdir  %{_sysconfdir}/%{name}
%global gratia_cron        %{_sysconfdir}/cron.d
%global gratia_datadir     %{_datadir}/%{name}
%global gratia_httpd       %{httpd}/%{name}
%global gratia_updates     %{gratia_varlib}/apel-updates
%global gratia_webapps     %{gratia_varlib}/webapps
%global gratia_tmp         %{gratia_varlib}/tmp
# Some variables for web access
%global httpd_conf /etc/httpd/conf/httpd.conf
%global httpd_port 8319

%define destdir %{buildroot}%{gratia_datadir}
mkdir -p %{destdir}
# -- gratia files ---
install -m 0754 %{source}/Downtimes.py             %{destdir}
install -m 0754 %{source}/InactiveResources.py     %{destdir}
install -m 0754 %{source}/InteropAccounting.py     %{destdir}
install -m 0754 %{source}/LCG.py                   %{destdir}
install -m 0754 %{source}/Rebus.py                 %{destdir}
install -m 0754 %{source}/SSMInterface.py          %{destdir}
install -m 0754 %{source}/configure-gratia-apel.sh %{destdir}
install -m 0754 %{source}/create-apel-index.sh     %{destdir}
install -m 0754 %{source}/update-svn.sh            %{destdir}
## install -m 0754 %{source}/lcg.sh                   %{destdir}

%define destdir %{buildroot}%{gratia_sysconfdir}
mkdir -p %{destdir}
install -m 0664 %{source}/lcg.conf            %{destdir}
install -m 0664 %{source}/lcg-db.conf         %{destdir}
install -m 0664 %{source}/lcg-reportableSites %{destdir}
install -m 0664 %{source}/lcg-reportableVOs   %{destdir}
mkdir -p %{destdir}/lcg-reportableSites.history
install -m 0664 %{source}/lcg-reportableSites.history/* %{destdir}/lcg-reportableSites.history

%define destdir %{buildroot}%{gratia_cron}
mkdir -p %{destdir}
install -m 0644 %{source}/gratia-apel.cron    %{destdir}

%define destdir %{buildroot}%{_initrddir}
mkdir -p %{destdir}
install -m 0755 %{source}/gratia-apel-cron    %{destdir}/gratia-apel-cron

%define destdir %{buildroot}%{gratia_updates}
mkdir -p %{destdir}
install -m 0644 %{source}/apel-updates/*    %{destdir}

%define destdir %{buildroot}%{gratia_webapps}
mkdir -p %{destdir}
install -m 0644 %{source}/webapps/*    %{destdir}
## mkdir -p %{buildroot}%{httpd}
## ln -s %{destdir}  %{gratia_httpd}

mkdir -p %{buildroot}%{gratia_tmp}
##mkdir -p %{buildroot}%{gratia_httpd}
mkdir -p %{buildroot}%{gratia_cron}
mkdir -p %{buildroot}%{gratia_logs}
mkdir -p %{buildroot}%{ssm_rundir}
touch %{buildroot}%{ssm_rundir}/ssm.pid

# -- ssm files ---
%define destdir %{buildroot}%{gratia_datadir}
mkdir -p %{destdir}
cp -pr  %{ssm_source}/ssm       %{destdir}
cp -pr %{ssm_source}/test       %{destdir}/ssm
chmod -R ug+x %{destdir}
chmod -R g-wx %{destdir}

%define destdir %{buildroot}%{gratia_sysconfdir}/ssm
mkdir -p %{destdir}
cp -p  %{ssm_source}/conf/*      %{destdir}
cp -p  %{ssm_source}/README      %{destdir}
mkdir -p %{buildroot}%{gratia_varlib}/messages

# --- gratia configuration file changes ----
%define destfile %{buildroot}%{gratia_sysconfdir}/lcg.conf
sed -i 's|HTTPDDIR|%{gratia_httpd}|'                    %{destfile}
sed -i 's|LOGDIR|%{gratia_logs}|'                       %{destfile}
sed -i 's|SSMFILE|%{gratia_datadir}/ssm/ssm_master.py|' %{destfile}
sed -i 's|SSMCONFIG|%{gratia_sysconfdir}/ssm/ssm.cfg|'  %{destfile}
sed -i 's|SSMUPDATESDIR|%{gratia_updates}|'             %{destfile}
sed -i 's|SYSCONFDIR|%{gratia_sysconfdir}|g'            %{destfile}

# --- gratia cron file changes ----
%define destfile %{buildroot}%{gratia_cron}/gratia-apel.cron
sed -i 's|LOCKFILE|%{gratia_lockfile}|g'       %{destfile}
sed -i 's|DATADIR|%{gratia_datadir}|g'         %{destfile}
sed -i 's|SYSCONFDIR|%{gratia_sysconfdir}|g'   %{destfile}

# --- gratia initd file changes ----
%define destfile %{buildroot}%{_initrddir}/gratia-apel-cron
sed -i 's|LOCKFILE|%{gratia_lockfile}|g'       %{destfile}

# --- ssm configuration file changes ----
%define destfile %{buildroot}%{gratia_sysconfdir}/ssm/ssm.cfg
sed -i "s%^pidfile:%pidfile: %{ssm_rundir}/ssm.pid\n#pidfile:%" %{destfile}
 sed -i "s%^path:%path: %{gratia_varlib}/messages\n#path:%"     %{destfile}
sed -i "s%^log-conf-file:%log-conf-file: %{gratia_sysconfdir}/ssm/ssm.log.cfg\n#log-conf-file:%"             %{destfile}
sed -i "s%^consumer-dn:%consumer-dn: /C=UK/O=eScience/OU=CLRC/L=RAL/CN=raptest.esc.rl.ac.uk\n#consumer-dn:%" %{destfile} 

%define destfile %{buildroot}%{gratia_sysconfdir}/ssm/ssm.log.cfg
sed -i "s%^args=%args=('%{gratia_logs}/ssm.log','a')\n#args=%"  %{destfile}


# Post processing
%post
ln -s %{gratia_webapps}  %{gratia_httpd}
rm -f %{gratia_datadir}/lcg.sh

# Files included
%files
%{gratia_datadir}
%{gratia_sysconfdir}
%config(noreplace) %{gratia_sysconfdir}/lcg.conf
%config(noreplace) %{gratia_sysconfdir}/lcg-db.conf
%config(noreplace) %{gratia_sysconfdir}/lcg-reportableSites
%config(noreplace) %{gratia_sysconfdir}/lcg-reportableVOs
%config(noreplace) %{gratia_sysconfdir}/ssm/ssm.cfg
%config(noreplace) %{gratia_sysconfdir}/ssm/ssm.log.cfg
%config            %{_sysconfdir}/cron.d/gratia-apel.cron
%{_sysconfdir}/rc.d/init.d/gratia-apel-cron
%{gratia_varlib}
## %{gratia_httpd}
%{gratia_logs}
%ghost %{ssm_rundir}/ssm.pid


# cleanup
%clean
## rm -rf $RPM_BUILD_ROOT


%changelog
* Mon Aug 27 2012 John Weigand <weigand@fnal.gov> - 1.00.1
Initial spec file



