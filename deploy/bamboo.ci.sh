#!/bin/bash
export PATH=/sbin:/bin:/usr/sbin:/usr/bin

install() {
 apt-get update
 apt-get install -y libpq-dev
 apt-get install -y python-dev
 apt-get install -y libxml2-dev
 apt-get install -y libxslt1-dev
 apt-get install -y python-pip
 apt-get install -y zip
 apt-get install -y python-setuptools
 pip install --upgrade awscli
}

install >/tmp/startup.log 2>&1

AWS_ACCESS_KEY_ID=${ACCESS_KEY} AWS_SECRET_ACCESS_KEY=${SECRET_KEY} aws s3 cp ${BAMBOODIR}/${ORIGARTIFACT} s3://cbmi_artifacts/${KEYNAME}/${DEVENV}/latest/$ARTIFACT

AWS_ACCESS_KEY_ID=${ACCESS_KEY} AWS_SECRET_ACCESS_KEY=${SECRET_KEY} aws s3 cp ${BAMBOODIR}/${ORIGARTIFACT} s3://cbmi_artifacts/${KEYNAME}/${DEVENV}/${BAMBOOBUILD}/$ARTIFACT
