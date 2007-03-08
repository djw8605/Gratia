
if [[ -z "$JAVA_HOME" ]] || [[ "$JAVA_HOME" == *jdk1.4* ]]; then
  if [[ -n "$VDT_LOCATION" ]] && [[ -d "$VDT_LOCATION/jdk1.5" ]]; then
    export JAVA_HOME="$VDT_LOCATION/jdk1.5"
    PATH="$VDT_LOCATION/jdk1.5/bin:$PATH"
    if [[ -n "$LD_LIBRARY_PATH" ]]; then
      LD_LIBRARY_PATH="/opt/vdt/jdk1.5/jre/lib/i386:/opt/vdt/jdk1.5/jre/lib/i386/server:/opt/vdt/jdk1.5/jre/lib/i386/client:$LD_LIBRARY_PATH"
    else
      export LD_LIBRARY_PATH="/opt/vdt/jdk1.5/jre/lib/i386:/opt/vdt/jdk1.5/jre/lib/i386/server:/opt/vdt/jdk1.5/jre/lib/i386/client"
    fi
  fi
fi
