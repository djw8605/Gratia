
Name: gratia-gold
Summary: A converter script from a Gratia database into Gold
Version: 1.2
License: ASL 2.0
Release: 1%{?dist}
Group: System Environment/Libraries

BuildArch: noarch
BuildRoot: %{_tmppath}/%{name}-%{version}-%{release}-buildroot

Source0: %{name}-%{version}.tar.gz

Requires: python-simplejson
Requires: MySQL-python
Requires: python-dateutil

%description
%{summary}

%prep
%setup -q

%build
python setup.py build

%install
rm -rf $RPM_BUILD_ROOT

python setup.py install --root=$RPM_BUILD_ROOT --record=INSTALLED_FILES

# Ghost files for the RPM.
mkdir -p $RPM_BUILD_ROOT/%_localstatedir/log/gratia-gold
touch $RPM_BUILD_ROOT/%_localstatedir/log/gratia-gold/gratia-gold.log

mkdir -p $RPM_BUILD_ROOT/%_localstatedir/lock/
touch $RPM_BUILD_ROOT/%_localstatedir/lock/gratia-gold.lock

%clean
rm -rf $RPM_BUILD_ROOT

%pre

getent group gold >/dev/null || groupadd -r gold
getent passwd gold >/dev/null || \
    useradd -r -g gold -d /var/lib/gratia-gold -s /sbin/nologin \
    -c "User for running gold" gold
exit 0

%files -f INSTALLED_FILES
%defattr(-,root,root)
%config(noreplace) %_sysconfdir/gratia-gold.cfg
%config(noreplace) %_sysconfdir/gratia_gold_rules.csv
%config(noreplace) %_sysconfdir/gratia_gold_blacklist.csv
%config(noreplace) %_sysconfdir/gratia_gold_whitelist.csv
%dir %_localstatedir/log/gratia-gold
%ghost %_localstatedir/log/gratia-gold/gratia-gold.log
%ghost %_localstatedir/lock/gratia-gold.lock

%changelog
* Fri Oct 18 2013 Srini Ramachandran <srini@fnal.gov> - 1.2
- Added cleanup of rollback_file and refund_file to avoid unnecessary rollback
* Thu Oct 17 2013 Srini Ramachandran <srini@fnal.gov> - 1.1
- If there's no starting point, start with gratia_max for the probe
- Set the gcharge machine to a customizable value (default: osg-xsede.grid.iu.edu)
- Enhanced README
* Mon Oct 14 2013 Srini Ramachandran <srini@fnal.gov> - 1.0-1
- Uncommented gcharge code to proceed with actual charging
* Fri Sep 13 2013 Srini Ramachandran <srini@fnal.gov> - 0.8-0
- Enhanced gratia-gold to process records for a set of hosts, and provided 
functionality for user defined rules on how to handle records for those hosts.

* Wed Jun 20 2012 Brian Bockelman <bbockelm@cse.unl.edu> - 0.5-1
- Finish implementation of gcharge callout.

* Tue Mar 06 2012 Brian Bockelman <bbockelm@cse.unl.edu> - 0.1-2
- Initial packaging of the gratia-gold package.

