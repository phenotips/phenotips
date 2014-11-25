#!/bin/bash

# ---------------------------------------------------------------------------
# See the NOTICE file distributed with this work for additional
# information regarding copyright ownership.
#
# This is free software; you can redistribute it and/or modify it
# under the terms of the GNU Lesser General Public License as
# published by the Free Software Foundation; either version 2.1 of
# the License, or (at your option) any later version.
#
# This software is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this software; if not, write to the Free
# Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
# 02110-1301 USA, or see the FSF site: http://www.fsf.org.
# ---------------------------------------------------------------------------

set -e

if [[ $1 != tomcat[67] ]]; then
  exit 1
fi

function restartTomcat {
  invoke-rc.d --quiet $1 restart || {
    RESULT=$?
    # Ignore if tomcat init script does not exist (yet)
    if [ $RESULT != 100 ]; then
      return $RESULT
    fi
  }
  return 0
}

# Reset proper rights
chown -R $1:$1 ${phenotips.base.path}/

tomcatConfig=/etc/default/$1

# Increase the amount of heap memory allocated for Tomcat, if needed
if { grep -q -E '^\s*JAVA_OPTS.*-Xmx' $tomcatConfig &>/dev/null; }; then
  # There is a value for Xmx, check if it is large enough
  CURRENT=`grep -E '^\s*JAVA_OPTS.*-Xmx' $tomcatConfig | sed -r -e 's/^\s*JAVA_OPTS.*-Xmx([^" ]+).*/\1/'`
  CURRENT_NUMBER=${CURRENT%[kKmMgG]}
  case $CURRENT in
    $CURRENT_NUMBER[kK]) CURRENT=$((CURRENT_NUMBER * 1024)) ;;
    $CURRENT_NUMBER[mM]) CURRENT=$((CURRENT_NUMBER * 1024 * 1024)) ;;
    $CURRENT_NUMBER[gG]) CURRENT=$((CURRENT_NUMBER * 1024 * 1024 * 1024)) ;;
  esac
  if (( CURRENT < (1024 * 1024 * 1024) )); then
    # Not large enough, update the value in place
    sed -i -r -e 's/^(\s*JAVA_OPTS.*-Xmx)([^" ]+)(.*)/\11g\3/' $tomcatConfig
  fi
elif { grep -q -E '^\s*JAVA_OPTS=' $tomcatConfig &>/dev/null; }; then
  # No Xmx configured, but there is a JAVA_OPTS setting, update it
  sed -i -r -e 's/^(\s*JAVA_OPTS=")(.*)/\1-Xmx1g \2/' $tomcatConfig
else
  # No JAVA_OPTS setting, add one with both Xmx and MaxPermSize
  echo -e -n '\nJAVA_OPTS="-Xmx1g -XX:MaxPermSize=192m"' >> $tomcatConfig
fi

# Increase the amount of permgen memory allocated for Tomcat, if needed
if { grep -q -E '^\s*JAVA_OPTS.*-XX:MaxPermSize=' $tomcatConfig &>/dev/null; }; then
  # There is a value for MaxPermSize, check if it is large enough
  CURRENT=`grep -E '^\s*JAVA_OPTS.*-XX:MaxPermSize=' $tomcatConfig | sed -r -e 's/^\s*JAVA_OPTS.*-XX:MaxPermSize=([^" ]+).*/\1/'`
  CURRENT_NUMBER=${CURRENT%[kKmMgG]}
  case $CURRENT in
    $CURRENT_NUMBER[kK]) CURRENT=$((CURRENT_NUMBER * 1024)) ;;
    $CURRENT_NUMBER[mM]) CURRENT=$((CURRENT_NUMBER * 1024 * 1024)) ;;
    $CURRENT_NUMBER[gG]) CURRENT=$((CURRENT_NUMBER * 1024 * 1024 * 1024)) ;;
  esac
  if (( CURRENT < (192 * 1024 * 1024) )); then
    # Not large enough, update the value in place
    sed -i -r -e 's/^(\s*JAVA_OPTS.*-XX:MaxPermSize=)([^" ]+)(.*)/\1192m\3/' $tomcatConfig
  fi
elif { grep -q -E '^\s*JAVA_OPTS=' $tomcatConfig &>/dev/null; }; then
  # No MaxPermSize configured, but there is a JAVA_OPTS setting, update it
  sed -i -r -e 's/^(\s*JAVA_OPTS=")(.*)/\1-XX:MaxPermSize=192m \2/' $tomcatConfig
fi

restartTomcat $1
