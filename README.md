## Current Build Status ##
Jenkins CI : [![Build Status](https://ci.phenotips.org/job/phenotips/badge/icon)](https://ci.phenotips.org/job/phenotips/)

# About #

This project mainly aims at providing **an easy way to collect clinical phenotypes using a standardized vocabulary**, thus allowing for **straight-forward cross-referencing with gene and disease databases**.

The patient data being managed includes:
* demographic information (name, date of birth, ...)
* family information and history (including pedigree)
* medical history
* prenatal and perinatal history
* measurements (with support for instant computation of percentiles and generation of growth charts)
* **clinical symptoms & physical findings**, with all phenotypic descriptions being mapped to a standardized vocabulary (the [Human Phenotype Ontology (HPO)](http://www.human-phenotype-ontology.org/))
* diagnosis (mapped to [OMIM](http://omim.org/))
* genetic variants found in the patient

The application provides a **web interface** accessible from any device equipped with a web browser and a secure connection to the application server. The complexity and technical codification of standardized phenotyping and disease vocabularies is hidden under the friendly UI, which allows for error-tolerant, predictive search of phenotypic descriptions, and provides instant suggestions of additional phenotypes and diseases matching the current phenotype selection to investigate.

## Major tools and resources involved used by this project ##
* The [Human Phenotype Ontology (HPO)](http://www.human-phenotype-ontology.org/) - a standardized vocabulary of phenotypic abnormalities encountered in human disease; contains approximately 10,000 terms and is being developed using information from [OMIM](http://omim.org/) and the medical literature
* [Apache Solr](http://lucene.apache.org/solr/) - an enterprise search platform
* [XWiki](http://xwiki.org) - an enterprise web application development framework

# Building instructions #

This project uses [Apache Maven](http://maven.apache.org/) for the whole lifecycle management. From the project's description:

> Apache Maven is a software project management and comprehension tool.
> Based on the concept of a project object model (POM), Maven can manage
> a project's build, reporting and documentation from a central piece of information.

In short, Maven handles everything related to building the project: downloading the required dependencies, compiling the Java files, running tests, building the jars or final zips, and even more advanced goals such as performing style checks, creating releases and deploying them to a remote repository. All these steps are configured declaratively with as little custom settings as possible, since the philosophy of maven is **"convention over configuration"**, relying on well-defined best practices and defaults, while allowing custom variations where needed.

Building the entire project is as simple as `mvn install`, but first the environment must be set-up:

* Make sure a proper JDK is installed, Oracle Java SE 1.6 or 1.7 is recommended. Just a JRE isn't enough, since the project requires compilation.
* Install maven by [downloading it](http://maven.apache.org/download.html) and following the [installation instructions](http://maven.apache.org/download.html#Installation).
* Clone the sources of the project locally, using one of:
    * `git clone git://github.com/phenotips/phenotips.git` if you need a read-only clone
    * `git clone git@github.com:phenotips/phenotips.git` if you also want to commit changes back to the project (and you have the right to do so)
    * download an [archive of the latest release](https://github.com/phenotips/phenotips/tags) if you don't want to use version control at all
* It is advisable to increase the amount of memory available to Maven: `export MAVEN_OPTS="-Xmx2g -XX:MaxPermSize=256m"`
* Execute `mvn install` at the command line to build the project
    * note that the first build will take a while longer, because all the required dependencies are downloaded, but all the subsequent builds should only take a few minutes

# Installation #

The project is split into several modules, among which `distribution/standalone` will result in a fully-working self-contained package ready to run. Running the application is as simple as:

* `mvn install`, as stated above, to get the project built
* go to the directory where the final package is located, `distribution/standalone/target`
* extract the contents of the `phenotips-standalone-1.0-SNAPSHOT.zip` archive to a location of your choice (outside the `target` directory, to ensure it is not overwritten by subsequent builds)
* launch the `start` script (`start.sh` on unix-like systems, `start.bat` on Windows)
* open [http://localhost:8080/](http://localhost:8080/) in a browser

# Issue tracker #

You can report issues, make feature requests and watch our progress on [our JIRA](https://phenotips.atlassian.net/projects/PT/issues/).

# Usage & limitations #

The standalone distribution comes packaged with a *Java Servlet Container*, [Jetty](http://www.eclipse.org/jetty/), a lightweight *relational database management system*, [HyperSQL](http://hsqldb.org/), and the *phenotyping application* itself.

# LICENSE #

PhenoTips and its related tools are distributed under the [AGPL version 3](http://www.gnu.org/licenses/agpl-3.0.html) (GNU Affero General Public License), a well known free software/open source license recognized both by the Free Software Foundation and the Open Source Initiative.
This means that every change made to the code must also be distributed under AGPL, and any composite works that build on top of PhenoTips must use a compatible license.
