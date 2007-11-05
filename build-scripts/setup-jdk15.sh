dropit=~greenc/bin/dropit
have_dropit=1

function have_dropit {
  if [[ -n "$have_dropit" ]] && [[ "$have_dropit" == "1" ]]; then
    return 0
  else
    return 1
  fi
}

function add_to_path() {
	[[ -n "$dropit" ]] || dropit=`type -p dropit`
  OPTIND=1 # must reset
  local dropit_args="-s -e" # default
  while getopts :sfep: OPT; do
    case $OPT in
      s)
	;;
      f)
	local dropit_f_arg="-f"
	;;
      e)
	;;
      p)
	local dropit_p_arg="-p"
	local dropit_path="$OPTARG"
	;;
    esac
  done
  shift $[ OPTIND - 1 ]
  if have_dropit; then
#    set -x
    printf "%s" `$dropit $dropit_args $dropit_f_arg $dropit_p_arg "$dropit_path" "$@"`
#    set +x
  else
    # Real simple: no removal of duplicates
    [[ -z "$dropit_path" ]] && dropit_path="$PATH"
    for dropit_frag in "$@"; do
      if [ -n "$dropit_f_arg" ]; then
        dropit_path="${dropit_frag}:${dropit_path}"
      else
        dropit_path="${dropit_path}:${dropit_frag}"
      fi
    done
    printf "%s" "${dropit_path}"
  fi
}

if [[ -z "$JAVA_HOME" ]] && [[ -d ~greenc/java/jdk1.5.0_11 ]]; then
  export JAVA_HOME=~greenc/java/jdk1.5.0_11
  export LD_LIBRARY_PATH=`add_to_path -p "$LD_LIBRARY_PATH" -f "$JAVA_HOME/lib/i386/"{,"server/","client/"}`
  PATH=`add_to_path -f "$JAVA_HOME/bin"`
fi

# Tell [X]Emacs that we want to be in sh-mode[bash] even though we don't
# have an interpreter line.
#
# Upon loading the file, [X]Emacs will ask for permission to evaluate 
# the form below. Satisfy yourself of the safety of this form 
# (eg C-h f sh-set-shell) before confirming execution.
### Local Variables:
### mode: sh
### eval: (sh-set-shell "bash" t nil)
### End:
