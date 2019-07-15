#!/bin/sh

phenotips_release_version="${1}"
phenotips_snapshot_version="${2}"

_const_example_string="Ex ./release_on_linux.sh 1.4.7 1.4"

if [ "${1}" = "--help" ] || [ "${1}" = "-h" ] ; then
    echo "Usage: release_on_linux.sh RELEASE_VERSION SNAPSHOT_VERSION"
    echo "${_const_example_string}"
    exit 0
fi

if [ -z "${phenotips_release_version}" ]; then
    echo "PhenoTips release version cannot be empty. ${_const_example_string}"
    exit 1
fi

if [ -z "${phenotips_snapshot_version}" ]; then
    echo "PhenoTips next SNAPSHOT version cannot be empty. ${_const_example_string}"
    exit 1
fi

export PROJECT=phenotips
export PT_VERSION="${phenotips_release_version}"
export SNAPSHOT_VERSION="${phenotips_snapshot_version}-SNAPSHOT"
export GPG_TTY="$(tty)"

while true; do
    printf "\nPROJECT=${PROJECT}\n"
    printf "PT_VERSION=${PT_VERSION}\n"
    printf "SNAPSHOT_VERSION=${SNAPSHOT_VERSION}\n"
    printf "GPG_TTY=${GPG_TTY}\n\n"

    printf "If the above variables look correct,\n"
    printf "Release PhenoTips [${PT_VERSION}]"
    printf " and set next SNAPSHOT version to [${SNAPSHOT_VERSION}]? [(y)es/(n)o]: "
    read -r yn

    case "${yn}" in
    [Yy]*)
      break
      ;;
    [Nn]*)
      exit 0
      break
      ;;
    *) echo "Please answer y (yes) or n (no) " ;;
    esac
done


git clean -dxf
mvn clean
git checkout -b "release-${PT_VERSION}"
sed -i -r -e "s/<phenotips.version>(.*)<\/phenotips.version>/<phenotips.version>${PT_VERSION}<\/phenotips.version>/" pom.xml
git add pom.xml
git commit -m '[release] Set the phenotips.version to the right value'
mvn release:prepare -DreleaseVersion="${PT_VERSION}" \
                    -DdevelopmentVersion="${SNAPSHOT_VERSION}" \
                    -Dtag="${PROJECT}-${PT_VERSION}" \
                    -Pfunctional-tests && \
                    git checkout "${PROJECT}-${PT_VERSION}" && \
                    git tag -d "${PROJECT}-${PT_VERSION}" && \
                    git tag -s -m "Tagging ${PROJECT}-${PT_VERSION}" "${PROJECT}-${PT_VERSION}" && \
                    git push -f origin "${PROJECT}-${PT_VERSION}" && \
                    mvn release:perform
