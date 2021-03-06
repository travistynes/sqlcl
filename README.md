# Sqlcl

Sqlcl is a Java application built with maven. It is meant to be used as a command line tool to query any database that has a JDBC driver, which is most relational databases.

This is useful when you want to query a database on the command line, over SSH, or don't have a graphical tool available.

#### Build

* Get [Maven](https://maven.apache.org/)
* Get a [JDK](https://jdk.java.net/11/) (mininum Java 1.8)
* Create a .mavenrc file in your home directory, and add JAVA_HOME to tell maven where your jdk is:

```
JAVA_HOME=/path/to/jdk
```

Build with: ```mvn package```

This produces the application jar file at ./target/sqlcl-x.x.jar.

Create a directory (we'll call it the application root) and place the jar there.

#### Configure

Create a **lib** directory in the application root. Place the JDBC driver for your database there.

Create a **config** directory in the application root with the following files:

* [application.properties](https://github.com/travistynes/sqlcl/blob/master/src/main/resources/application.properties)
* [logback.xml](https://github.com/travistynes/sqlcl/blob/master/src/main/resources/logback.xml)

Update application.properties with the connection information for your database. The logback.xml file can be changed to enable additional logging to suit your needs.

#### How to use

Run: ```java -Dloader.path="./lib" -jar ./sqlcl-x.x.jar```

The db.sh file is provided to make invoking Sqlcl easier:

```./db.sh```

Edit the [db.sh](https://github.com/travistynes/sqlcl/blob/master/db.sh) script to set the correct Java location.

You can add the application to your PATH and create a symlink so it behaves like a regular linux command. This is how I use it, and is very convenient:

```
export PATH=$PATH:/path/to/sqlcl
ln -s db.sh db
```

Sqlcl reads the query from stdin and writes the result to stdout:

```echo "select current_timestamp from dual" | db```

Or save your query in a file and pipe it into Sqlcl, and pipe the result to another command:

```cat query.sql | db | vim -```

Or

```db < query.sql | vim - ```

#### Properties

You can create multiple properties files of the form **application-profileName.properties** to specify connections to different databases. For example, given the following properties files:

* config/application-dev.properties
* config/application-prod.properties

You can query either database with:

```
cat query.sql | db --env=dev
cat query.sql | db --env=prod
```

By default, Sqlcl will use the **config/application.properties** file if the --env option is not specified.

#### Database metadata

Query database metadata (list tables, describe table columns, and indices):

```
db --tables=schema
db --describe=schema.table
db --index=schema.table
```

#### Multi-statement support

Multiple statements separated by semicolons will be executed in order, within a single transaction. If any statement fails, the transaction is rolled back. If all of the statements are successful, the transaction is committed.

Sqlcl is a small tool that tries to do one thing well, and works best when chained together with other commands (see: [Unix philosophy](https://en.wikipedia.org/wiki/Unix_philosophy)).

This code is free to use for any purpose.
