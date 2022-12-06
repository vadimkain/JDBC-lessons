import org.postgresql.Driver;
import util.ConnectionManager;

import java.sql.SQLException;

/**
 * <h1>Statement. DML операции</h1>
 * Дело в том, что через execute мы получаем только true или false. Но помним, что стандарты SQL говорят о том, что
 * при выполнении INSERT/UPDATE/DELETE нам возвращается количество обновлённых/вставленных/удалённых строк. Через
 * операцию execute нам пришлось бы доплнительно вызывать метод (<i>execute.getUpdateCount()</i>).
 * <br><br>
 * На практике для DML операций чаще используется <b>executeUpdate()</b>.
 * <br>
 */
public class JdbcRunner {
    public static void main(String[] args) {
        Class<Driver> driverClass = Driver.class;
        /*
        String sql = """
                INSERT INTO info (data) VALUES ('Test1'), ('Test2'), ('Test3'), ('Test4');
                """;
         */
        String sql = """
                UPDATE info SET data = 'TestTest' WHERE id = 5 RETURNING *;
                """; // RETURNING * возвращаем сразу же значения обновленных строк, но с executeUpdate не работает
        try (
                var connection = ConnectionManager.open();
                var statement = connection.createStatement();
        ) {
            System.out.println(connection.getSchema());
            System.out.println(connection.getTransactionIsolation());
            var executeResult = statement.executeUpdate(sql); // Выполнение DML запроса
            System.out.println(executeResult);
            // Возвращаем кол-во строк, над которыми выполнили операции
            System.out.println(statement.getUpdateCount());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
