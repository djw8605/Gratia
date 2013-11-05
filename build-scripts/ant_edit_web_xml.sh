#!/bin/sh

#####This script takes the path to a web.xml which needs to be modified for gratia-reporting.war#####

if [ $# -lt 1 ] ; then
      echo "Need the path to web.xml !"
      exit 0
fi

#####Create two temporary files#####
tmp=`mktemp ${TMPDIR:-/tmp}/gratia-build.XXXXXXXXXX`; \
tmp2=`mktemp ${TMPDIR:-/tmp}/gratia-build.XXXXXXXXXX`; \
	[[ -n "$tmp" ]] || exit 1; \
	[[ -n "$tmp2" ]] || exit 1; \
	trap "[[ -n \"$tmp\" ]] && rm -f \"$tmp\" \"$tmp2\"" EXIT; \

#####Use perl to make changes to web.xml and store in "$tmp"#####
perl -we 'use strict; my $seen_par; my $par = "BIRT_VIEWER_TIMEZONE"; my $val = "GMT"; while (<>) { m&<param-name>\Q$par\E</param-name>& and $seen_par = 1; $seen_par and s&<param-value>.*&<param-value>$val</param-value>& and undef $seen_par; print }' $1/web.xml > "$tmp"; \
  (( $? == 0 )) || exit 1; \

#####Use perl to make changes to "$tmp" and store in "$tmp2"#####
perl -we 'use strict; my $seen_par; my $par = "WORKING_FOLDER_ACCESS_ONLY"; my $val = "false"; while (<>) { m&<param-name>\Q$par\E</param-name>& and $seen_par = 1; $seen_par and s&<param-value>.*&<param-value>$val</param-value>& and undef $seen_par; print }' "$tmp" > "$tmp2"; \
  (( $? == 0 )) || exit 1; \

#####Use perl to make changes to "$tmp2" and store in "$tmp"#####
perl -wpe 'use strict; m&display-name& and do { print "  <!-- UrlRewriteFilter configuration -->\n <filter>\n	<filter-name>UrlRewriteFilter</filter-name>\n	<filter-class>org.tuckey.web.filters.urlrewrite.UrlRewriteFilter</filter-class>\n	<init-param>\n	 <param-name>logLevel</param-name>\n	 <param-value>WARN</param-value>\n	 </init-param>\n </filter>\n <filter-mapping>\n	 <filter-name>UrlRewriteFilter</filter-name>\n	 <url-pattern>/*</url-pattern>\n </filter-mapping>\n\n <!-- Gratia Reporting Service Listener -->\n        <listener>\n                <listener-class>net.sf.gratia.reporting.ReportingService</listener-class>\n        </listener>\n\n"; print "    <servlet>\n        <servlet-name>MonitorStatus</servlet-name>\n        <servlet-class>net.sf.gratia.administration.MonitorStatus</servlet-class>\n        <load-on-startup>2</load-on-startup>\n    </servlet>\n\n    <servlet-mapping>\n        <servlet-name>MonitorStatus</servlet-name>\n        <url-pattern>/monitor-status.html</url-pattern>\n    </servlet-mapping>\n\n"};' "$tmp2" > "$tmp"; \
  (( $? == 0 )) || exit 1; \

#####Copy web.xml to web.xml.old and copy $tmp contents to web.xml#####
cp -p $1/web.xml $1/web.xml.old; \
cat "$tmp" > $1/web.xml;
