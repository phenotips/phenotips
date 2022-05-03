<p align="center">
  <img src="https://repository-images.githubusercontent.com/2685841/e3982780-032e-11ea-8d50-149013256d78" width="600px" alt="PhenoTips Open Source logo"/>
</p>

<p align="center">
  <img src="https://github.com/phenotips/phenotips/workflows/Maven%20build/badge.svg" alt="Maven build">
  <a href="https://opensource.org/licenses/AGPL-3.0" target="_blank">
    <img src="https://img.shields.io/badge/license-AGPL--3.0-blue.svg" alt="AGPL-3.0">
  </a>
  <img src="https://img.shields.io/badge/made%20in-Canada-red.svg" alt="Made in Canada">
</p>


## This repository is no longer maintained

The open-source, XWiki version of the PhenoTips platform is no longer actively developed or maintained.

PhenoTips development is now focused on the closed-source Enterprise solution available at [phenotips.com](https://phenotips.com/).

## A customizable patient data platform for genomic medicine

PhenoTips is a browser-based tool for recording linked clinical phenotype, genetic, disease, and family history data for patients with genetic diseases. PhenoTips includes a highly-customizable patient form, and includes sections to enter:
* clinical symptoms & physical findings ([HPO](http://www.human-phenotype-ontology.org/))
* family information and history (including pedigree)
* diagnosis (mapped to [OMIM](http://omim.org/) or [Orphanet](http://www.orphadata.org/))
* genes and variants of interest
* measurements (with support for instant computation of percentiles and generation of growth charts)
* demographic information (name, date of birth, ...)

## Usage

You use PhenoTips directly through your **web browser**.

* If you are running a standalone version of PhenoTips directly on your computer, you can usually find it at `localhost:8080`
* If you are at a hospital or in a research group with your own PhenoTips instance, you should ask someone what URL to go to in order to access your instance

## Powered by
* [HPO](http://www.human-phenotype-ontology.org/) - phenotype ontology
* [OMIM](http://omim.org/) - disease terminology
* [Orphanet](http://www.orphadata.org/) - disease ontology
* [MONDO](http://obofoundry.org/ontology/mondo.html) - disease ontology
* [Apache Solr](http://lucene.apache.org/solr/) - an enterprise search platform
* [XWiki](http://xwiki.org) - an enterprise web application development framework

## Building from source

This project uses [Apache Maven](http://maven.apache.org/) for lifecycle management.

First set up your environment:

* Make sure a proper JDK is installed, Java SE 1.8 or higher. Just a JRE isn't enough, since the project requires compilation.
* Install [Apache Maven](http://maven.apache.org/)
* Clone the source code: `git clone git@github.com:phenotips/phenotips.git`
* Ensure Maven has enough memory: `export MAVEN_OPTS="-Xmx2g -XX:MaxPermSize=256m"`

Then, build the project with:
```
cd phenotips/
mvn install
```
*Note: the first build may take a long time because all the dependencies are downloaded, but subsequent builds should only take a few minutes*

## Running a custom built instance

The project is split into several modules, among which `distribution/standalone` will result in a fully-working self-contained package ready to run. Running the application is as simple as:

* `mvn install`, as stated above, to build the project
* go to the directory where the final package is located, `distribution/standalone/target`
* extract the contents of the `phenotips-standalone-<version>.zip` archive to a location of your choice (outside the `target` directory, to ensure it is not overwritten by subsequent builds)
* launch the `start` script (`start.sh` on unix-like systems, `start.bat` on Windows)
* open a browser and go to [http://localhost:8080/](http://localhost:8080/)
* this is a completely local version, not connected to any other PhenoTips instance or other software in your institution; a default user is provided, you can log in using User: `Admin` and Password: `admin` (case-sensitive)

## Support

The open-source community version of PhenoTips is no longer maintained or supported.

For information about the Enterprise version, please see [phenotips.com](https://phenotips.com/).

## License

This open-source version of PhenoTips was distributed under the [AGPL version 3](http://www.gnu.org/licenses/agpl-3.0.html) (GNU Affero General Public License), a well known free software/open source license recognized both by the Free Software Foundation and the Open Source Initiative.

You can ensure compliance with this license by:
* making sure every change made to the code is also be distributed under AGPL, and any works that integrate PhenoTips (even over APIs) use a compatible license
* including prominent notice of the use of PhenoTips in any software that uses it
* retaining all copyright notices in the software

For more information, please [Contact Us](https://phenotips.com/contact.html).
