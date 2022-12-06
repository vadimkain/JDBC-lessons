import org.postgresql.Driver;
import util.ConnectionManager;

import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * <h1>Statement. DDL операции</h1>
 * Для того, чтобы отправить запрос в БД у нас есть три варианта:
 * <ol>
 *     <li>
 *         <b>Statement</b> - сам интерфейс. Он так-же наследуется от AutoCloseable, а значит его также надо
 *         закрывать после выполнения запроса, как и соединение.
 *     </li>
 *     <li>
 *         <b>CallableStatement</b> - не используем, т.к. он служит нам для выполнения хранимых процедур.
 *     </li>
 *     <li>
 *         <b>PreparedStatement</b> - в реальной практике чаще всего используют именно этот класс, потому что он
 *         более универсальный и наследуется от Statement.
 *     </li>
 * </ol>
 * Для того чтобы выполнить универсальный запрос есть обычный метод <i>execute(String):boolean</i>, но он не очень
 * удобен в плане получения какой-то информации, поэтому у нас есть другие похожие методы такие как например
 * <i>executeUpdate(String):int</i>, который так же принимает строку и возвращает int, а не boolean. И так же
 * <i>executeQuery(String):ResultSet</i>, который используется для выполнения SELECT'ов.
 * <ul>
 *     <li>
 *         <b>execute(String):boolean</b> - универсальный, чаще для DDL операций
 *     </li>
 *     <li>
 *         <b>executeQuery(String):ResultSet</b> - для SELECT'ов
 *     </li>
 *     <li>
 *         <b>executeUpdate(String):int</b> - для DML операций (INSERT UPDATE DELETE)
 *     </li>
 *     <li>
 *         <b>executeLargeUpdate(String):long</b> - для больших DML операций
 *     </li>
 * </ul>
 */
public class JdbcRunner {
    public static void main(String[] args) {
        Class<Driver> driverClass = Driver.class;
        /*
        String sql = """
                CREATE DATABASE game;
                """;
        String sql = """
                DROP DATABASE game;
                """;
        String sql = """
                CREATE SCHEMA game;
                """;

         */
        String sql = """
                CREATE TABLE IF NOT EXISTS info (
                    id SERIAL PRIMARY KEY,
                    data TEXT NOT NULL
                );
                """;
        try (
                var connection = ConnectionManager.open();
                var statement = connection.createStatement();
        ) {
            System.out.println(connection.getSchema());
            System.out.println(connection.getTransactionIsolation());
            var executeResult = statement.execute(sql); // Выполнение DDL запроса
            System.out.println(executeResult);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
