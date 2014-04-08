#!/bin/bash
#####################################################################
# John Weigand (9/26/12)
#
# Script for creating RPM more easily
#####################################################################
function logit {
  echo "$1"
}
#--------------------
function logerr {
  logit "ERROR: $1";exit 1
}
#--------------------
function runit {
  local cmd="$1"
  logit "RUNNING: $cmd"
  $cmd;rtn=$?
  [ "$rtn" != "0" ] && logerr "Command failed.. return code: $rtn"
}
#--------------------
function ask_continue {
  echo -n "$1 y/n: "
  read ans
  if [ "$ans" != "y" ];then 
    logit "... bye";exit 1
  fi
}
#--------------------
function remove_topdir {
  [ -d "$RPM_TOPDIR" ] && runit "rm -rf $RPM_TOPDIR"
}
#--------------------
function setup_rpmbuild {
  logit "--------------------------------------"
  logit "... setting up rpmbuild environment: 
  $RPM_TOPDIR"
  if [ -d "$RPM_TOPDIR" ];then
    ask_continue "$RPM_TOPDIR already exists
... can we remove it?"
    remove_topdir
  fi
  dirs="BUILD RPMS SOURCES SPECS SRPMS TMP"
##  dirs="RPMS SOURCES SPECS SRPMS TMP"
  for dir in $dirs
  do
    runit "mkdir -p $RPM_TOPDIR/$dir"
  done
##  runit "ln -s `pwd` $RPM_TOPDIR/BUILD"
  logit "Setup of rpmbuild environment complete" 
  logit "--------------------------------------"
}
#--------------------
function setup_rpmmacros {
  logit "------------------------------------"
  logit "Setting up rpmmacros"
  export RPMMACROS_EXISTS=0
  if [ -f $RPMMACROS ]
  then
    export RPMMACROS_EXISTS=1
    mv $RPMMACROS $RPMMACROS.save
  fi
  cat >$RPMMACROS <<EOF
%_topdir    $RPM_TOPDIR
%_builddir  %{_topdir}/BUILD
%_sourcedir %{_topdir}/SOURCES
%_rpmdir    %{_topdir}/RPMS
%_specdir   %{_topdir}/SPECS
%_srcrpmdir %{_topdir}/SRPMS
%_tmppath   %{_topdir}/TMP
EOF
  logit "$(cat $RPMMACROS)"
  logit "Setting up rpmmacros complete"
  logit "------------------------------------"
}
#--------------------
function build_source_tarball {
    logit "------------------------------------"
    logit "Build the source tar for rpm_build"
    local tmpdir=/tmp/$PGM.$$
    runit "rm -rf   $tmpdir"
    runit "mkdir -p $tmpdir/$TOPDIR"
    runit "cp -pr * $tmpdir/$TOPDIR/."
    runit "unzip -d $tmpdir/$TOPDIR $TARBALLS/$SSM_TARBALL"
    runit "rm -rf $tmpdir/tarballs"
    runit "rm -rf $tmpdir/rpmbuild"
    logit "...... eliminating .svn entries if any"
    cd $tmpdir
    find . -type d -name ".svn" -exec rm -rf {} \;
    tarball=$TARBALLS/$GRATIA_TARBALL
    logit "... building tarball: $tarball"
    runit "tar -czf $tarball ."
    logit "$(ls -hl $tarball)"
    cd -
    logit "... removing $tmpdir"
    runit "rm -rf $tmpdir"
    runit "cp $TARBALLS/$GRATIA_TARBALL $RPM_TOPDIR/SOURCES/."
    logit "Source tar file built"
    logit "------------------------------------"
}
## #--------------------
## function setup_spec_files {
##   runit "cp $BUILD_HOME/rpmspecs/gratia-apel.spec $RPM_TOPDIR/SPECS/."
## }
#--------------------
function build_rpms {
    logit "------------------------------------"
    logit "Building rpm"
    [ ! -f "$SPEC_FILE" ] && logerr "spec file does not exist:
  $SPEC_FILE"
    runit "rpmbuild -ba $SPEC_FILE"
    logit;logit "... saving rpms to $BUILD_HOME/rpms"
    runit "mkdir -p $BUILD_HOME/rpms"
    runit "cp -r $RPM_TOPDIR/RPMS/* $BUILD_HOME/rpms"
    runit "cp -r $RPM_TOPDIR/SRPMS/* $BUILD_HOME/rpms"
    logit "Building rpm complete"
    logit "------------------------------------"
}
#--------------------
function cleanup {
    # Remove custom .rpmmacros and restore original .rpmmacros if necessary
    logit "------------------------------------"
    logit "Cleaning up"
    rm -f $RPMMACROS
    if [[ $RPMMACROS_EXISTS -eq 1 ]]; then
        runit "mv $RPMMACROS.save $RPMMACROS"
    fi
    # remove the rpmbuild directory
    runit "rm -rf $RPM_TOPDIR"
    logit "Cleanup complete"
    logit "------------------------------------"
}
#--------------------
function show_rpms {
  logit "--------------------------------"
  logit "RPMS:
`find $BUILD_HOME/rpms -name "*.rpm" -exec ls -l {} \;`
"
  logit "--------------------------------"
}
#--------------------
function clean_old_build {
  logit "--------------------------------"
  logit "Cleaning out previous build"
  dir=$RPMS
  if [ -d "$dir" ];then
     logit "... deleting $dir"
     runit "rm -rf $dir"
  fi
  tarball=$TARBALLS/$GRATIA_TARBALL
  if [ -f "$tarball" ];then
     logit "... deleting $tarball"
     runit "rm -f $tarball"
  fi
  logit "Cleaning out previous build complete"
  logit "--------------------------------"
}
#--------------------
function validate {
  [ ! -f "$BUILD_HOME/$PGM" ] && \
      logerr "You need to run this from the directory $PGM is located in"
}
#### MAIN ##############################
PGM=`basename $0`
export BUILD_HOME=`pwd`
export TOPDIR=gratia-apel
export SPEC_FILE=$BUILD_HOME/rpmspecs/gratia-apel.spec
export RPMMACROS=$HOME/.rpmmacros
export GRATIA_TARBALL=gratia-apel.tar.gz
export SSM_TARBALL=ssm-1.2-1.zip
#---- work dirs ----
export RPM_TOPDIR=$BUILD_HOME/rpmbuild
export RPMS=$BUILD_HOME/rpms
export TARBALLS=$BUILD_HOME/tarballs

validate
clean_old_build 
setup_rpmbuild
build_source_tarball
setup_rpmmacros
## setup_spec_files  ## not sure of the benefit of this
build_rpms
cleanup
show_rpms

echo "DONE"
exit 0


