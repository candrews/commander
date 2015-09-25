# Commander
[![Build Status](https://travis-ci.org/candrews/commander.svg?branch=master)](https://travis-ci.org/candrews/commander)
[![GNU Affero General Public License version 3.0](https://img.shields.io/github/license/candrews/commander.svg)](https://www.gnu.org/licenses/agpl-3.0.html)

Commander is a voice control system offering extensive extensibility via plugins.

## Building from source
Availability uses a Maven build system.

### Prerequisites

Git, JDK 8, and Maven.

Be sure that your `JAVA_HOME` environment variable points to the `jdk1.8.0` folder
extracted from the JDK download.

### Check out sources
`git clone git@github.com:candrews/commander.git`

### Compile and test
`mvn install`

### Run the project
To run a default, demo configuration:
```shell
mvn exec:java -pl core
```
To run any other configuration:
```shell
mvn exec:java -pl core -Dcommander.configuration=<path>
```
where path can be a URL or a file system path.

A web interface is also available - run
```
mvn exec:java -pl web
```
or, to provide any other configuration:
```
mvn exec:java -pl web -Dcommander.configuration=<path>
```

Use your browser to hit http://localhost:8080/

## Import into an IDE (Eclipse, IntelliJ, etc)
This project uses [Lombok](https://projectlombok.org/) so special instructions have to be followed when using most IDEs.
