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

echo "UNZIP STANDALONE AND PACKAGE EXTENSIONS"
cd ~
rm -rf extension.zip
rm -rf ${BAMBOODIR}/extension.zip
unzip -oq "${BAMBOODIR}/distribution/standalone/target/phenotips-standalone-1.2-SNAPSHOT.zip" >/tmp/startup.log 2>&1
cd phenotips-standalone-1.2-SNAPSHOT/data/extension
find -name *.xed -exec sed -e 's/<installed.installed type="boolean">true<\/installed.installed>/<installed.installed type="boolean">false<\/installed.installed>/' -i \{\} \;
zip -r ~/extension.zip .
cp -rfp ~/extension.zip ${BAMBOODIR}/

echo "ZIP MAVEN REPO"
cd ~
rm -rf mavenrepo.zip
rm -rf ${BAMBOODIR}/mavenrepo.zip
zip -r mavenrepo.zip .m2
cp -rfp ~/mavenrepo.zip ${BAMBOODIR}/

md5=`md5sum ${BAMBOODIR}/${ORIGARTIFACT}` | cut -d ' ' -f 1

echo "UPLOAD EXTENSION ZIP"
AWS_ACCESS_KEY_ID=${ACCESS_KEY} AWS_SECRET_ACCESS_KEY=${SECRET_KEY} aws s3api put-object --bucket cbmi-artifacts --key ${KEYNAME}/${DEVENV}/latest/extension.zip --body ${BAMBOODIR}/extension.zip
AWS_ACCESS_KEY_ID=${ACCESS_KEY} AWS_SECRET_ACCESS_KEY=${SECRET_KEY} aws s3api put-object --bucket cbmi-artifacts --key ${KEYNAME}/${DEVENV}/${BAMBOOBUILD}/extension.zip --body ${BAMBOODIR}/extension.zip

echo "UPLOAD MAVEN REPO ZIP"
AWS_ACCESS_KEY_ID=${ACCESS_KEY} AWS_SECRET_ACCESS_KEY=${SECRET_KEY} aws s3api put-object --bucket cbmi-artifacts --key ${KEYNAME}/${DEVENV}/latest/mavenrepo.zip --body ${BAMBOODIR}/mavenrepo.zip
AWS_ACCESS_KEY_ID=${ACCESS_KEY} AWS_SECRET_ACCESS_KEY=${SECRET_KEY} aws s3api put-object --bucket cbmi-artifacts --key ${KEYNAME}/${DEVENV}/${BAMBOOBUILD}/mavenrepo.zip --body ${BAMBOODIR}/mavenrepo.zip

echo "UPLOAD PHENOTIPS"
AWS_ACCESS_KEY_ID=${ACCESS_KEY} AWS_SECRET_ACCESS_KEY=${SECRET_KEY} aws s3api put-object --metadata md5=$md5 --bucket cbmi-artifacts --key ${KEYNAME}/${DEVENV}/latest/$ARTIFACT --body ${BAMBOODIR}/${ORIGARTIFACT}
AWS_ACCESS_KEY_ID=${ACCESS_KEY} AWS_SECRET_ACCESS_KEY=${SECRET_KEY} aws s3api put-object --metadata md5=$md5 --bucket cbmi-artifacts --key ${KEYNAME}/${DEVENV}/${BAMBOOBUILD}/$ARTIFACT --body ${BAMBOODIR}/${ORIGARTIFACT}
