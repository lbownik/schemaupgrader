# Relational database schema versioning in 55 lines of code

## Abstract
The article describes an approach to reliably version relational database schemas.

## Introduction
The database schema versioning is a problem often though last during 
implementation of database backed applications (like web applications or
 micro services). Sometimes even the [basic rules](https://odetocode.com/blogs/scott/archive/2008/01/30/three-rules-for-database-work.aspx) get broken.
 The application code gets carefully versioned under version control,
 but the database schema versioning is neglected. Database creation 
scripts dumped from ERD tool get stuffed into [code repository](https://odetocode.com/blogs/scott/archive/2008/01/31/versioning-databases-the-baseline.aspx) in hope that 
this is enough. More advanced teams provide [delta scripts](https://odetocode.com/blogs/scott/archive/2008/02/02/versioning-databases-change-scripts.aspx) used to upgrade
 the production databases, which are already populated with data, and cannot
 be simply dropped and recreated from scratch. Often the database schema 
upgrade record is being tracked manually within tools like Jira, MS Excel
 or just plain text file lying on DBAs desktop.These approaches are neither 
repeatable nor reliable.

## Solution
On internet one can find solutions for managing database schema 
modification DDL scripts like [flywaydb.org](https://flywaydb.org/). 
These tools are fine but they impose complexity into the building process. 
The proposed solution, on the other hand, is more a convention then code 
and assumes that one:
* keepsall database creation and modification DDL statements as a set of
 functions expressed in a programming language;
* uses separate database table to keep track of schema versions;
* forces the application to check schema version at startup and makes it fail
 to start if mismatch gets detected;
* providesthe application with a distinguished runtime mode 
(denoted for example by command line parameter) to upgrade the database schema.

## Implementation
The proposed solution has been implemented as a BSD licensed micro
 library called "[chemaupgrader](https://github.com/lbownik/schemaupgrader)". The library consists of a single 
source file [SchemaUpgrader.java](https://github.com/lbownik/schemaupgrader/blob/master/src/schemaupgrader/SchemaUpgrader.java) (55 lines of code excluding
 comments and blank lines) which provides two static functions:
* **getVersionOf** - which returns the current version of database schema, and
* **upgradeVersion** - which upgrades database schema to a provided version and 
handles version tracking in a designated table called "versions".

The following code snippet shows proper application of these functions.

```
import java.sql.Connection;
import static schemaupgrader.SchemaUpgrader.*;
import static java.util.Arrays.asList;

public class Main {

   private final static int EXPECTED_DB_VERSION = 1;
   /****************************************************************************
    *
    ***************************************************************************/
   public static void main(String[] args) throws Exception {

      finalJDBCDataSource ds = null; // I surely should have initialized this

      try (final Connection c = ds.getConnection()) {
         if (asList(args).contains("upgradeDB")) {
            upgradeVersion(c, EXPECTED_DB_VERSION, DatabaseVersions::build);
         } else if (getVersionOf(c) != EXPECTED_DB_VERSION) {
            throw new AssertionError("Database version mismatch.");
         }
      }
      //rest of the application logic here
   }
}
```
As has been stated before the main idea is to **check the version 
of the database schema first** and **exit an application if the version does 
not match the expected one**. Alternatively an upgrade of the application happens
 if a special command line switch is provided. The schema upgrade may be also
 called from an alternative application entry point (another class with "main"
 function) or an entirely separate application if many applications share the 
same database.
