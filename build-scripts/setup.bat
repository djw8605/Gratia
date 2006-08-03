#
# set root = source code base - e.g. - /eclipse/workspace/gratia
#
set base=\eclipse\workspace
set root=\eclipse\workspace\gratia
#
# now - start construction classpath
#
set classpath=.
set classpath=%classpath%;%root%\services
set classpath=%classpath%;%root%\storage
set classpath=%classpath%;%root%\servlets
set classpath=%classpath%;%root%\security
set classpath=%classpath%;%root%\soap
set classpath=%classpath%;%root%\administration
set classpath=%classpath%;%base%%\GratiaReporting\src
