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

PROP_workDir="${project.build.directory}/${project.build.finalName}"
PROP_iconFile="${iconFile}"
PROP_volumeIconFile="${volumeIconFile}"
PROP_volumeBackgroundFile="${volumeBackgroundFile}"
PROP_iconSize="${iconSize}"
PROP_bundleName="${bundleName}"
PROP_dmgDir="${dmgDir}"
PROP_dmgFile="${project.build.finalName}.dmg"
PROP_bundleDir="${bundleDir}"
PROP_includeApplicationsSymlink="${includeApplicationsSymlink}"
PROP_internetEnable="${internetEnable}"

[ -n "$PROP_workDir" -a -d "$PROP_workDir" ] ||
  { echo "[ERROR] Invalid work directory \"$PROP_workDir\""; exit 1; }

PATH="$PATH:/Developer/Tools:/Applications/Xcode.app/Contents/Developer/Tools"

cd "$PROP_workDir"

if [ -n "$PROP_iconFile" ]; then
  [ -e "$PROP_bundleDir/Contents/Resources/$PROP_iconFile" ] ||
    { echo "[ERROR] File \"$PROP_iconFile\" does not exist"; exit 1; }
  /usr/libexec/PlistBuddy -c "add :CFBundleIconFile string '$PROP_iconFile'" "$PROP_bundleDir/Contents/Info.plist" &>/dev/null ||
    { echo "[ERROR] Failed to update Info.plist"; exit 1; }
fi

if [ -n "$PROP_volumeIconFile" ]; then
  mv "$PROP_bundleDir/Contents/Resources/$PROP_volumeIconFile" "$PROP_dmgDir/.VolumeIcon.icns" &>/dev/null ||
    { echo "[ERROR] Failed to copy file \"$PROP_volumeIconFile\" to \"$PROP_dmgDir\"";  exit 1; }
else
  rm -f "$PROP_dmgDir/.VolumeIcon.icns" &>/dev/null
fi

if [ -n "$PROP_volumeBackgroundFile" ]; then
  { mkdir -p "$PROP_dmgDir/.background" &&
      mv "$PROP_bundleDir/Contents/Resources/$PROP_volumeBackgroundFile" "$PROP_dmgDir/.background/background.png"; } &>/dev/null ||
    { echo "[ERROR] Failed to copy file \"$PROP_volumeBackgroundFile\" to \"$PROP_dmgDir\"";  exit 1; }
  SetFile -a V "$PROP_dmgDir/.background" &>/dev/null
else
  rm -rf "$PROP_dmgDir/.background" &>/dev/null
fi

if [ "$PROP_includeApplicationsSymlink" = "true" ]; then
  ln -f -h -s /Applications "$PROP_dmgDir/ " &>/dev/null ||
    { echo "[ERROR] Failed to create link to \"/Applications\""; exit 1; }
else
  rm -f "$PROP_dmgDir/ " &>/dev/null
fi

SetFile -a B "$PROP_bundleDir" &>/dev/null

function createDiskImage {
  local dmgFile dmgFormat

  if [ -n "$PROP_volumeIconFile" -o -n "$PROP_volumeBackgroundFile" ]; then
    dmgFile="tmp_$PROP_dmgFile"
    dmgFormat=UDRW
  else
    dmgFile="$PROP_dmgFile"
    dmgFormat=UDZO
  fi

  hdiutil create -format $dmgFormat -fs "HFS+" -volname "$PROP_bundleName" -srcfolder "$PROP_dmgDir" -ov "$dmgFile" || return 1
  [ "$dmgFile" = "$PROP_dmgFile" ] && return 0

  if hdiutil attach "$dmgFile" -mountpoint "$PROP_dmgDir"; then
    if [ -n "$PROP_volumeIconFile" ]; then
      SetFile -a C "$PROP_dmgDir" || return 1
    fi

    local bgWidth bgHeight setBounds setIconSize setPicture setBundlePosition setAppsPosition iconSize=${PROP_iconSize:-100}

    if [ -n "$PROP_volumeBackgroundFile" ]; then
      bgWidth=`sips -g pixelWidth "$PROP_dmgDir/.background/background.png" | grep pixelWidth | awk '{print $2;}'`
      bgHeight=`sips -g pixelHeight "$PROP_dmgDir/.background/background.png" | grep pixelHeight | awk '{print $2;}'`
    fi
    (( (bgWidth < (iconSize * 4)) || (bgHeight < (iconSize * 2)) )) && bgWidth=0
    (( bgWidth > 0 )) &&
      setBounds="set the bounds of container window to {200, 100, $((200 + bgWidth)), $((100 + bgHeight))}" &&
      setIconSize="set icon size of viewOptions to $iconSize" &&
      setPicture="set background picture of viewOptions to file \".background:background.png\"" &&
      setBundlePosition="set position of item \"$PROP_bundleName.app\" of container window to {$((bgWidth / 2 - iconSize * 5 / 4)), $((bgHeight / 2))}"
    (( bgWidth > 0 )) && [ "$PROP_includeApplicationsSymlink" = "true" ] &&
      setAppsPosition="set position of item \" \" of container window to {$((bgWidth / 2 + iconSize * 5 / 4)), $((bgHeight / 2))}"

    echo '
      tell application "Finder"
        tell disk "'$PROP_dmgDir'"
          open
          set current view of container window to icon view
          set toolbar visible of container window to false
          set statusbar visible of container window to false
          '$setBounds'
          set viewOptions to the icon view options of container window
          set arrangement of viewOptions to not arranged
          '$setIconSize'
          '$setPicture'
          '$setBundlePosition'
          '$setAppsPosition'
          close
          open
          update without registering applications
          delay 2
        end tell
      end tell
    ' | osascript &&
    hdiutil detach "$PROP_dmgDir" &&
    hdiutil convert "$dmgFile" -format UDZO -o "$PROP_dmgFile" &&
    rm "$dmgFile" ||
    return 1
  else
    return 1
  fi

  return 0
}

createDiskImage &>/dev/null ||
  { echo "[ERROR] Failed to create disk image \"$PROP_dmgFile\""; exit 1; }

if [ "$PROP_internetEnable" = "true" ]; then
  hdiutil internet-enable -yes "$PROP_dmgFile" &>/dev/null ||
    { echo "[ERROR] Failed to enable download post-processing of disk image \"$PROP_dmgFile\""; exit 1; }
fi

mv "$PROP_dmgFile" ..
exit 0
