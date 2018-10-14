//
//Copyright (C) 2014-2017 Łukasz Bownik
//
//Permission is hereby granted, free of charge, to any person obtaining a copy of this software and 
//associated documentation files (the "Software"), to deal in the Software without restriction, including 
//without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell 
//copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the 
//following conditions:
//
//The above copyright notice and this permission notice shall be included in all copies or substantial 
//portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT 
//LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
//IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
//HETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
//SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
package schemaupgrader;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/*******************************************************************************
 * @author lukasz.bownik@gmail.com
 ******************************************************************************/
public final class SchemaUpgrader {
   /****************************************************************************
    * Checks the database schema version.
    * @param c database connection
    * @return database schema version
    * @throws SQLException is something bad happens
    ***************************************************************************/
   public static int getVersionOf(final Connection c)
           throws SQLException {

      try (final Statement s = c.createStatement()) {
         try (final ResultSet rs = s.executeQuery("select max(number) from versions")) {
            return rs.next() ? rs.getInt(1) : 0;
         } catch (final SQLException e) {
            return -1;
         }
      }
   }
   /****************************************************************************
    * Upgrade database schema version to the given one.
    * @param c database connection
    * @param targetVersion target database version
    * @param builder a funtion capable of vuilding any schema version
    * @throws Exception is something bad happens
    ***************************************************************************/
   public static void upgradeVersion(final Connection c, final int targetVersion,
           final VersionBuilder builder)
           throws Exception {

      if (targetVersion < 0) {
         throw new IllegalArgumentException("Expected version < 0.");
      }
      final int actualVersion = getVersionOf(c);
      if (actualVersion > targetVersion) {
         throw new AssertionError("Actual schema version = "
                 + actualVersion + " is greater than target version.");
      }
      buildVersion(c, actualVersion, targetVersion, builder);
   }
   /****************************************************************************
    *
    ***************************************************************************/
   private static void buildVersion(final Connection c, int actualVersion,
           final int targetVersion, final VersionBuilder builder)
           throws Exception {

      if (actualVersion == -1) {
         createVersionsTable(c);
         actualVersion = 0;
      }
      for (int v = actualVersion + 1; v <= targetVersion; v++) {
         try {
            builder.build(c, v);
            appendVersion(c, v);
            c.commit();
         } catch (final Exception e) {
            c.rollback();
            throw e;
         }
      }
   }
   /****************************************************************************
    *
    ***************************************************************************/
   private static void createVersionsTable(final Connection c)
           throws SQLException {

      try (final Statement s = c.createStatement()) {
         s.execute("create table versions(number integer primary key not null, ts timestamp default now)");
      }
   }
   /****************************************************************************
    *
    ***************************************************************************/
   private static void appendVersion(final Connection c, final int ver)
           throws SQLException {

      try (final Statement s = c.createStatement()) {
         s.execute("insert into versions values(" + ver + ", now())");
      }
   }
   /****************************************************************************
    *
    ***************************************************************************/
   public interface VersionBuilder {

      public void build(final Connection c, final int version) throws Exception;
   }
}
