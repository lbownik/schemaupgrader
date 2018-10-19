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
package example;

import java.sql.Connection;
import static fluentJDBC.FluentConnection.using;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/*******************************************************************************
 * @author lukasz.bownik@gmail.com
 ******************************************************************************/
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
         s.execute("CREATE table users(name varchar(20) primary key, pass varchar(20))");
         s.execute("insert into users values('\ta', 'b')");
      }
   }
   /****************************************************************************
    *
    ***************************************************************************/
   static void v2_createLogsTable(final Connection c) throws SQLException {

      using(c).prepare("CREATE table logs(ts bigint primary key,"
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
