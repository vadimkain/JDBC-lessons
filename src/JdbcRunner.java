import org.postgresql.Driver;
import util.ConnectionManager;

import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * <h1>Подключение к базам данных</h1>
 * Класс Connection наследуется от AutoCloseable, т.е. необходимо закрывать все соединения, которые используем
 * чтобы предотвращать все утечки памяти.
 * <br><br>
 * Для хорошего тона выносим соединение в отдельный Util класс, все сведения о нём там же.
 */
public class JdbcRunner {
    public static void main(String[] args) {
        Class<Driver> driverClass = Driver.class;

        try (var connection = ConnectionManager.open();) {
            System.out.println(connection.getTransactionIsolation());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
