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
* keeps all database creation and modification DDL statements as a set of
 functions expressed in a programming language;
* uses separate database table to keep track of schema versions;
* forces the application to check schema version at startup and makes it fail
 to start if mismatch gets detected;
* providesthe application with a distinguished runtime mode 
(denoted for example by command line parameter) to upgrade the database schema.

## Implementation
The proposed solution has been implemented as a BSD licensed micro
 library called "[schemaupgrader](https://github.com/lbownik/schemaupgrader)". The library consists of a single 
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

In order to perform the actual schema
 changes the _upgradeSchema_ function requires a pointer to a builder 
function that is able to build the requested schema version by applying 
the set of changes to the previous one.The implementation is totally up to
 the developer, but for clarity it is important to keep every version "patch"
 in a separate function. The example implementation may look like the 
following (to issue SQL commands I used another micro library of mine
 that can be found [**here**](https://github.com/lbownik/fluentjdbc) but it is 
not mandatory).
```
import java.sql.Connection;
import static fluentJDBC.FluentConnection.using;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public final class DatabaseVersions {
   /****************************************************************************
    *
    ***************************************************************************/
   public static void build(final Connection c, final int version)
         throws Exception {

      switch (version) {
         case 1:
            v1_createUsersTable(c);
            return;
         case 2:
            v2_createLogsTable(c);
            return;
         case 3:
            v3_trimUserNames(c);
            return;
      }
   }
   /****************************************************************************
    * 
    ***************************************************************************/
   static void v1_createUsersTable(final Connection c) throws SQLException {

      try (final Statement s = c.createStatement()) {
         s.execute("create table users(name varchar(20) primary key, pass varchar(20))");
         s.execute("insert into users values('\ta', 'b')");
      }
   }
   /****************************************************************************
    *
    ***************************************************************************/
   static void v2_createLogsTable(final Connection c) throws SQLException {

      using(c).prepare("create table logs(ts bigint primary key,"
            + "user varchar(20),"
            + "msg varchar(200),"
            + "foreign key (user) references users(name))").andUpdate();
   }
   /****************************************************************************
    *
    ***************************************************************************/
   static void v3_trimUserNames(final Connection c) throws Exception {

      final List<String> userNames = using(c).prepare("select name from users").
            andMap((rs) -> rs.getString(1));

      for (final String name : userNames) {
         using(c).prepare("update users set name = ? where name = ?").
               set(name.trim()).set(name).andUpdate();
      }
   }
}
```
In this example the first function creates "users" table with a 
single user. The second function adds "logs" table. The third function 
does not change the schema itself, but is used to clean the data already 
populating the database.The obvious constraint of this approach is that
 one may NEVER modify the existing functions that have been deployed to 
production, as this will cause loss of consistency.

## Pros and cons
The proposed solution exhibits the following advantages:
* DDL code is controlled along with application code (synching a working copy
 with the repository will always result with newest set schema _version patch 
functions_, since they are code);
* the process is reliable (if the database version does not match the expected
 version, the application will not start, upgrading database, on the other hand,
 guarantees proper schema version alignment);
* the process is repeatable (all schema changes are applied in the same manner: 
add _version patch function_, update expected version constant, build application ,
run application against existing database), and does not require any additional 
build steps;
* the solution is flexible (version patches can upgrade schema, migrate data, 
clean data, transform data, etc.);
* the DDL code is testable - every _version patch function_ can be put under test
 harness; the following listing shows such test.
```
@Test public void upgradeVersion_upgradesDatabase_forProperInvocation()
         throws Exception {

   upgradeVersion(this.c, 3, DatabaseVersions::build);

   assertEquals(3, getVersionOf(this.c));
   assertEquals(new Integer(3),
            using(c).prepare("select count(*) from versions").
            andMapOne((rs) ->rs.getInt(1)).get());
   assertEquals("b",
            using(c).prepare("select pass from users where name = 'a'").
            andMapOne((rs) ->rs.getString(1)).get());
   assertFalse(using(c).prepare("select pass from users where name = '\ta'").
               andMapOne((rs) ->rs.getString(1)).isPresent());
 }
```
The proposed solution exhibits the following disadvantages:
* no SQL DDL script that could be fetched into ERD tool - this is widely 
offset by the ability of tools to dump schema from live database;
* no SQL syntax checks during development as the SQL code is represented by
 string constant - offset by testability;
* database schema upgrades or data cleaning action may take long for big 
databases - DDL scripts expose the same issue;
* _version patch functions_ unit tests may take long time, because they need to
 target actual database.

## Conclusion
The proposed solution aims to solve the relational database schema versioning
 problem in a most streamlined and lightweight way without imposing any new 
build artifacts and build steps. It is mostly convention over tools, backed
 by two utility functions placed in a single BDS licensed [java file](https://github.com/lbownik/schemaupgrader/blob/master/src/schemaupgrader/SchemaUpgrader.java) that one 
can included in a project and use freely.
