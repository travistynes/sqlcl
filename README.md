# Sqlcl

Sqlcl is a Java application built with maven. It is meant to be used as a command line tool to query any database that has a JDBC driver, which is most relational databases.

This is especially useful when you do not have access to the database, but you do have SSH access to a machine that has access to the database.

#### Build

Build: ```mvn package```

Run:

```java -Dloader.path="./lib" -jar ./sqlcl-1.0.jar```

You can also run from maven without building: ```mvn spring-boot:run```

The db.sh file is provided to make invoking Sqlcl easier:

```./db.sh```

You can add the application to your PATH and create a symlink so it behaves like a regular linux command:

```
export PATH=$PATH:/path/to/sqlcl
ln -s db.sh db
```

#### Configure

Create a **lib** directory in the application root. Place the JDBC driver for your database there.

Create a **config** directory in the application root with the following files:

* [application.properties](https://github.com/travistynes/sqlcl/blob/master/src/main/resources/application.properties)
* [logback.xml](https://github.com/travistynes/sqlcl/blob/master/src/main/resources/logback.xml)

Update application.properties with the connection information for your database. The logback.xml file can be changed to enable additional logging to suit your needs.

Edit the [db.sh](https://github.com/travistynes/sqlcl/blob/master/db.sh) script to set the correct Java location.

#### How to use

Sqlcl reads the query from stdin and writes the result to stdout:

```echo "select current_timestamp from dual" | db```

Or save your query in a file and pipe it into Sqlcl, and pipe the result to another command:

```cat query.sql | db | vim -```

Time your query execution:

```time query.sql | db```

You can create multiple properties files of the form **application-profileName.properties** to specify connections to different databases. For example, given the following properties files:

* application-dev.properties
* application-prod.properties

You can query either database with:

```cat query.sql | db --env=dev```
```cat query.sql | db --env=prod```

Query database metadata (list tables, describe table columns, and indices):

```db --tables=schema```
```db --describe=schema.table```
```db --index=schema.table```

Sqlcl is a small tool that tries to do one thing well, and works best when chained together with other commands (see: [Unix philosophy](https://en.wikipedia.org/wiki/Unix_philosophy)).

This code is free to use for any purpose.
