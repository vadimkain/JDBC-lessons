import org.postgresql.Driver;
import util.ConnectionManager;

import java.sql.SQLException;

/**
 * <h1>ResultSet. Операция SELECT</h1>
 * Это самая сложная операция по манипулированию с Java-объектами для того чтобы получить результат нашей выборки.
 * <br><br>
 * <b>executeQuery()</b> возвращает ResultSet.
 * <br><br>
 * Мы получили некую реализацию из нашего драйвера: <i>org.postgresql.jdbc.PgResultSet@12cdcf4</i>
 * <br><br>
 * Интерфейс <b>ResultSet</b> наследуется от AutoCloseable, а значит его тоже надо закрывать. Более того, он
 * содержит невероятно большое количество методов для того чтобы манипулировать нашими данными. Кроме того чтобы
 * получать какие-то данные из нашего результата выборки (много get методов), есть ещё и методы которые не стоит
 * использовать (<i>update/insert</i>).
 * <br><br>
 * ResultSet очень похож на итератор (<b>Java Core</b>: Iterator это интерфейс, в котором есть два основных метода -
 * hasNext() и next(). Реализация этого интерфейса проходится по структуре данных и проверяется через hasNext, есть
 * ли следующий элемент в коллекции. Если есть, то через метод next получаем этот объект).
 * <br>
 * В случае с ResultSet мы так же проходимся по результатам выборки (а это перечень строк), но в случае с ResultSet
 * у нас есть только один метод: <b>next():boolean</b>. По сути, next() совмещает в себе и hasNext и next нашего
 * итератора, только в данном случае нам достаточно будет его одного. Следовательно, если вернемся в реализацию
 * и попробуем вывести результат нашей выборки, то используем цикл, если ожидаем, что получим много записей.
 * А если берём только какое-то поле по ключу, то ожидаем какую-то одну строку.
 * <br><br>
 * <h2>Как устроен ResultSet</h2>
 * <img src='ResultSet.png'/>
 * <br>
 * Как только сделали нашу выборку (вызвали метод executeQuery), то получили объект ResultSet и он стоит как будто
 * перед результатом нащей выборки. И так как он очень похож на итератор, то для того чтобы узнать, есть ли
 * какой-то результат у нас, то мы должны вызывать next, который вернёт true или false до того как мы не вызвали
 * next первый раз, мы не имеем права вызывать get'теры. Как только вызвали next(), он переходит на следующую
 * итерацию и становится будто <b>между строками</b>, и теперь чере геттеры можем получать значения из строки.
 * Далее, после того как всё достали опять делаем запрос next() и опять переводим ResultSet на состояние между строк,
 * и вновь можем выполнять геттеры из только что пройденной строки и т.д., пока следующий вызов next не вернёт нам
 * false.
 * <br><br>
 * Более того, можно делать DML операции, но лучше так не делать, т.к. executeQuery в основном используется для
 * SELECT'ов.
 */
public class JdbcRunner {
    public static void main(String[] args) {
        Class<Driver> driverClass = Driver.class;
        String sql = """
                SELECT * FROM flight_storage.ticket
                """;
        try (
                var connection = ConnectionManager.open();
                var statement = connection.createStatement();
        ) {
            System.out.println(connection.getSchema());
            System.out.println(connection.getTransactionIsolation());

            // SELECT запрос
            var executeResult = statement.executeQuery(sql);

            while (executeResult.next()) {
                System.out.println(
                        executeResult.getLong("id") + " " + // Возвращаем id
                        executeResult.getString("passenger_no") + " " + // Возвращаем passenger_no
                        executeResult.getBigDecimal("cost") // Возвращаем cost
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
