import util.ConnectionManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * <h1>SQL Injection</h1>
 * При чём тут SQL Injection? Дело в том, что {@code Statement} редко пользуются, либо не пользуются вообще и
 * дают предпочтение именно {@code PreparedStatement} из-за SQL инъекций. Т.е. по сути это что-то вроде хакерских
 * атак, когда вместе с параметром (в данном случае это параметр {@code String flightId}). Поэтому эта строчка может
 * быть передана на сервер (в метод) видоизменнёной. Т.е. можем вместо "2" сказать: {@code 2 OR 1 = 1}. Т.е. тот
 * пользователь, который знает SQL может ввести логическое выражение и передать условие, которое 100% вернёт true,
 * следовательно, таким образом вместо того чтобы получить наши билеты по id 2, можем получить все наши билеты, потому
 * что условие true верно.
 * <br><br>
 * <b>Следовательно,</b> можно сделать вывод, что SQL Injection это техника, которая может нанести вред или уничтожить
 * базу данных, потому что можем кроме как логических условий делать и более сложные условия, например такие как DROP.
 * <br><br>
 * <b>Для того чтобы обезопасить наш запрос, существует <i>PreparedStatement</i></b>, который проверяет все SQL
 * инъекции и не позволяет выполнять эти запросы.
 */
public class JdbcRunner {

    /**
     * Используя
     * <pre>{@code String flightId = "2 OR 1 = 1; DROP TABLE info;";}</pre>
     * получим ошибку, потому что последняя операция DROP TABLE и ResultSet ввиду этого не возвращается.
     * @param args
     */
    public static void main(String[] args) {
        // String flightId = "2";
        // String flightId = "2 OR 1 = 1";
        String flightId = "2 OR 1 = 1; DROP TABLE info;";
        var result = getTicketsByFlightId(flightId);
        System.out.println(result);
    }

    /**
     * <h2>Получаем список билетов по заданному <i>flightId</i></h2>
     * getLong может вернуть null, что неправльно, т.к. примитив всегда должен быть не пустым. Здесь лучше подойдёт
     * getObject, который принимает название колонки и тип, в который хотим преобразовать колонку. В таком случае он
     * прекрасно работает с null'ами. Потому что null характерны для сложных типов, а не для примитивных. Поэтому
     * использум {@code getObject()}
     * <pre>{@code result.add(resultSet.getObject("id", Long.class));}</pre>
     * @param flightId
     * @return
     * Получаем строки из таблицы по заданному ключу
     */
    private static List<Long> getTicketsByFlightId(String flightId) {
        String sql = """
                SELECT id FROM flight_storage.ticket WHERE flight_id = %s
                """.formatted(flightId);
        List<Long> result = new ArrayList<>();
        try (
                var connection = ConnectionManager.open();
                var statement = connection.createStatement();
        ) {
            var resultSet = statement.executeQuery(sql);

            while (resultSet.next()) {
                result.add(resultSet.getObject("id", Long.class));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return result;
    }
}
