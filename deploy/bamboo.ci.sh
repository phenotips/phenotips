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

cd ~
unzip ${BAMBOODIR}/distribution/standalone/target/phenotips-standalone-1.2-SNAPSHOT.zip
cd phenotips-standalone-1.2-SNAPSHOT/data/extension
find -name *.xed -exec sed -e 's/<installed.installed type="boolean">true<\/installed.installed>/<installed.installed type="boolean">false<\/installed.installed>/' -i \{\} \;
zip -r ~/extension.zip .
cp -rfp ~/extension.zip ${BAMBOODIR}/

md5=`md5sum ${BAMBOODIR}/${ORIGARTIFACT}` | cut -d ' ' -f 1

AWS_ACCESS_KEY_ID=${ACCESS_KEY} AWS_SECRET_ACCESS_KEY=${SECRET_KEY} aws s3api put-object --metadata md5=$md5 --bucket cbmi-artifacts --key ${KEYNAME}/${DEVENV}/latest/$ARTIFACT --body ${BAMBOODIR}/${ORIGARTIFACT}
AWS_ACCESS_KEY_ID=${ACCESS_KEY} AWS_SECRET_ACCESS_KEY=${SECRET_KEY} aws s3api put-object --metadata md5=$md5 --bucket cbmi-artifacts --key ${KEYNAME}/${DEVENV}/${BAMBOOBUILD}/$ARTIFACT --body ${BAMBOODIR}/${ORIGARTIFACT}
