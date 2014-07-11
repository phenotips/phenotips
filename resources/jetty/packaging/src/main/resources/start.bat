@echo off
REM -------------------------------------------------------------------------
REM See the NOTICE file distributed with this work for additional
REM information regarding copyright ownership.
REM
REM This is free software; you can redistribute it and/or modify it
REM under the terms of the GNU Lesser General Public License as
REM published by the Free Software Foundation; either version 2.1 of
REM the License, or (at your option) any later version.
REM
REM This software is distributed in the hope that it will be useful,
REM but WITHOUT ANY WARRANTY; without even the implied warranty of
REM MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
REM Lesser General Public License for more details.
REM
REM You should have received a copy of the GNU Lesser General Public
REM License along with this software; if not, write to the Free
REM Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
REM 02110-1301 USA, or see the FSF site: http://www.fsf.org.
REM -------------------------------------------------------------------------


REM -------------------------------------------------------------------------
REM Optional ENV vars
REM -----------------
REM   START_OPTS - parameters passed to the Java VM when running Jetty
REM     e.g. to increase the memory allocated to the JVM to 1GB, use
REM       set START_OPTS=-Xmx1024m
REM -------------------------------------------------------------------------

setlocal EnableDelayedExpansion

set JETTY_HOME=jetty
if not defined START_OPTS set START_OPTS=-Xmx512m -XX:MaxPermSize=192m

if not defined JETTY_PORT (
  set JETTY_PORT=%1
  if not defined JETTY_PORT (
    set JETTY_PORT=8080
  )
)

if not defined JETTY_STOP_PORT (
  set JETTY_STOP_PORT=%2
  if not defined JETTY_STOP_PORT (
    set JETTY_STOP_PORT=8079
  )
)

echo Starting Jetty on port %JETTY_PORT%, please wait...

REM Location where XWiki stores generated data and where database files are.
set XWIKI_DATA_DIR=data
set START_OPTS=%START_OPTS% -Dxwiki.data.dir=%XWIKI_DATA_DIR%

REM Ensure the data directory exists so that XWiki can use it for storing permanent data.
if not exist %XWIKI_DATA_DIR% mkdir %XWIKI_DATA_DIR%

REM Ensure the logs directory exists as otherwise Jetty reports an error
if not exist %XWIKI_DATA_DIR%\logs mkdir %XWIKI_DATA_DIR%\logs

REM Ensure the work directory exists so that Jetty uses it for its temporary files.
if not exist %JETTY_HOME%\work mkdir %JETTY_HOME%\work

REM Specify port on which HTTP requests will be handled
set START_OPTS=%START_OPTS% -Djetty.port=%JETTY_PORT%

REM Specify Jetty's home directory
set START_OPTS=%START_OPTS% -Djetty.home=%JETTY_HOME%

REM Specify port and key to stop a running Jetty instance
set START_OPTS=%START_OPTS% -DSTOP.KEY=xwiki -DSTOP.PORT=%JETTY_STOP_PORT%

REM Specify the encoding to use
set START_OPTS=%START_OPTS% -Dfile.encoding=UTF8

REM Path to the solr configuration
set START_OPTS=%START_OPTS% -Dsolr.solr.home=solrconfig

REM In order to avoid getting a "java.lang.IllegalStateException: Form too large" error
REM when editing large page in XWiki we need to tell Jetty to allow for large content
REM since by default it only allows for 20K. We do this by passing the
REM org.eclipse.jetty.server.Request.maxFormContentSize property.
REM Note that setting this value too high can leave your server vulnerable to denial of
REM service attacks.
set START_OPTS=%START_OPTS% -Dorg.eclipse.jetty.server.Request.maxFormContentSize=1000000

set JETTY_CONFIGURATION_FILES=
for /r %%i in (%JETTY_HOME%\etc\jetty-*.xml) do set JETTY_CONFIGURATION_FILES=!JETTY_CONFIGURATION_FILES! "%%i"

java %START_OPTS% %3 %4 %5 %6 %7 %8 %9 -jar %JETTY_HOME%/start.jar %JETTY_HOME%/etc/jetty.xml %JETTY_CONFIGURATION_FILES%
