#Relational database schema versioning in 55 lines of code
##Abstract
The article describes an approach to reliably version relational database schemas.
##Introduction
The database schema versioning is a problem often though last during 
implementation of database backed applications (like web applications or
 micro services). Sometimes even the [basic rules](https://odetocode.com/blogs/scott/archive/2008/01/30/three-rules-for-database-work.aspx) get broken.
 The application code gets carefully versioned under version control,
 but the database schema versioning is neglected. Database creation 
scripts dumped from ERD tool get stuffed into [code repository] (https://odetocode.com/blogs/scott/archive/2008/01/31/versioning-databases-the-baseline.aspx) in hope that 
this is enough. More advanced teams provide [delta scripts](https://odetocode.com/blogs/scott/archive/2008/02/02/versioning-databases-change-scripts.aspx) used to upgrade
 the production databases, which are already populated with data, and cannot
 be simply dropped and recreated from scratch. Often the database schema 
upgrade record is being tracked manually within tools like Jira, MS Excel
 or just plain text file lying on DBAs desktop.These approaches are neither 
repeatable nor reliable.