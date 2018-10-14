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
import java.sql.DriverManager;
import java.sql.SQLException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static schemaupgrader.SchemaUpgrader.*;
import static fluentJDBC.FluentConnection.using;
import example.DatabaseVersions;

/*******************************************************************************
 * @author lukasz.bownik@gmail.com
 ******************************************************************************/
public class SchemaUpgraderUseCases {
   /****************************************************************************
    *
    ***************************************************************************/
   @Before
   public void setUp() throws Exception {

      this.c = DriverManager.getConnection("jdbc:hsqldb:mem:test", "SA", "");
   }
   /****************************************************************************
    *
    ***************************************************************************/
   @After
   public void tearDown() throws Exception {

      if (!this.c.isClosed()) {
         using(this.c).prepare("DROP SCHEMA PUBLIC CASCADE").andUpdate();
         this.c.close();
      }
   }
   /****************************************************************************
    *
    ***************************************************************************/
   @Test
   public void getVersionOf_throwsNullPointer_forNullConnection()
         throws Exception {

      try {
         getVersionOf(null);
         fail();
      } catch (final NullPointerException e) {
         assertTrue(true);
      } catch (final Exception e) {
         fail();
      }
   }
   /****************************************************************************
    *
    ***************************************************************************/
   @Test
   public void getVersionOf_throwsException_forClosedConnection()
         throws Exception {

      tearDown();
      try {
         getVersionOf(this.c);
         fail();
      } catch (final SQLException e) {
         assertTrue(true);
      } catch (final Exception e) {
         fail();
      }
   }
   /****************************************************************************
    *
    ***************************************************************************/
   @Test
   public void getVersionOf_returnMinusOne_forEmptyDatabase()
         throws Exception {

      assertEquals(-1, getVersionOf(this.c));
   }
   /****************************************************************************
    *
    ***************************************************************************/
   @Test
   public void getVersionOf_returnZero_forInitializedDatabase()
         throws Exception {

      upgradeVersion(this.c, 0, null);

      assertEquals(0, getVersionOf(this.c));
      assertEquals(new Integer(0),
            using(c).prepare("select count(*) from versions").
                  andMapOne((rs) -> rs.getInt(1)).get());
   }
   /****************************************************************************
    *
    ***************************************************************************/
   @Test
   public void upgradeVersion_throwsIllegalArgumet_forNegativeVersion()
         throws Exception {

      try {
         upgradeVersion(this.c, -1, null);
         fail();
      } catch (final IllegalArgumentException e) {
         assertEquals("Expected version < 0.", e.getMessage());
      } catch (final Exception e) {
         fail();
      }
   }
   /****************************************************************************
    *
    ***************************************************************************/
   @Test
   public void upgradeVersion_upgradesDatabse_forProperInvocation()
         throws Exception {

      upgradeVersion(this.c, 1, DatabaseVersions::build);

      assertEquals(1, getVersionOf(this.c));
      assertEquals(new Integer(1),
            using(c).prepare("select count(*) from versions").
                  andMapOne((rs) -> rs.getInt(1)).get());
      assertEquals("b",
            using(c).prepare("select pass from users where name = '\ta'").
                  andMapOne((rs) -> rs.getString(1)).get());
      
      upgradeVersion(this.c, 3, DatabaseVersions::build);

      assertEquals(3, getVersionOf(this.c));
      assertEquals(new Integer(3),
            using(c).prepare("select count(*) from versions").
                  andMapOne((rs) -> rs.getInt(1)).get());
      assertEquals("b",
            using(c).prepare("select pass from users where name = 'a'").
                  andMapOne((rs) -> rs.getString(1)).get());
      assertFalse(using(c).prepare("select pass from users where name = '\ta'").
            andMapOne((rs) -> rs.getString(1)).isPresent());
   }
   /****************************************************************************
    *
    ***************************************************************************/
   @Test
   public void upgradeVersion_throwsAssertionError_forLowerTargetVersion()
         throws Exception {

      upgradeVersion(this.c, 1, DatabaseVersions::build);

      try {
         upgradeVersion(this.c, 0, null);
         fail();
      } catch (final AssertionError e) {
         assertEquals("Actual schema version = 1 is greater than target version.",
               e.getMessage());
      } catch (final Exception e) {
         fail();
      }
   }
   /****************************************************************************
    *
    ***************************************************************************/
   @Test
   public void upgradeVersion_doesNothing_forTargetVersionEqualToCurrentVersion()
         throws Exception {

      upgradeVersion(this.c, 1, DatabaseVersions::build);

      upgradeVersion(this.c, 1, DatabaseVersions::build);

      assertEquals(1, getVersionOf(this.c));
      assertEquals(new Integer(1),
            using(c).prepare("select count(*) from versions").
                  andMapOne((rs) -> rs.getInt(1)).get());
      assertEquals("b",
            using(c).prepare("select pass from users where name = '\ta'").
                  andMapOne((rs) -> rs.getString(1)).get());
   }
   /****************************************************************************
    *
    ***************************************************************************/
   @Test
   public void upgradeVersion_throwsExcpetion_forClosedConnection()
         throws Exception {

      tearDown();

      try {
         upgradeVersion(this.c, 3, DatabaseVersions::build);
         fail();
      } catch (final SQLException e) {
         assertTrue(true);
      } catch (final Exception e) {
         fail();
      }
   }
   /****************************************************************************
    *
    ***************************************************************************/
   @Test
   public void upgradeVersion_throwsNullPointer_forNullBuilder()
         throws Exception {

      try {
         upgradeVersion(this.c, 3, null);
         fail();
      } catch (final NullPointerException e) {
         assertTrue(true);
      } catch (final Exception e) {
         fail();
      }
   }
   /****************************************************************************
    *
    ***************************************************************************/
   @Test
   public void upgradeVersion_doesNothing_whenBuilderThrowsException()
         throws Exception {

      upgradeVersion(this.c, 1, DatabaseVersions::build);

      try {
         upgradeVersion(this.c, 2, (c, v) -> {
            throw new Exception("test");
         });
         fail();
      } catch (final Exception e) {
         assertEquals("test", e.getMessage());
         assertEquals(1, getVersionOf(c));
      }
   }
   /****************************************************************************
    *
    ***************************************************************************/
   @Test
   public void upgradeVersion_throwsNullPointer_forNullConnection()
         throws Exception {

      upgradeVersion(this.c, 1, DatabaseVersions::build);

      try {
         upgradeVersion(null, 1, DatabaseVersions::build);
         fail();
      } catch (final NullPointerException e) {
         assertTrue(true);
      } catch (final Exception e) {
         fail();
      }
   }
   /****************************************************************************
    *
    ***************************************************************************/
   private Connection c;
}
