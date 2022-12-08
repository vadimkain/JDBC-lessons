import util.ConnectionManager;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * <h1>FetchSize</h1>
 * {@code FetchSize} является ключевым в производительности приложения работы с БД. Как только мы делаем запрос и
 * получаем большую выборку, то {@code FetchSize = 3} говорит о том, что мы сначала берём только три строки из
 * всей нашей выборки и через соединение получаем их из БД в приложение. За тем, после того как <b>курсор</b> ResultSet
 * прошёлся по выборке, у соединённого БД берём ещё 3 строки. Так, рекурсивно проходимся и проверяем, есть ли там
 * данные из следующей пачки или нет. Если есть - отправляем в Java приложение.
 * <br><br>
 * Если указывать довольно маленький {@code FetchSize}, то приложение будет очень часто обращаться к БД, что не есть
 * хорошо. Обычно следует выбирать в пределах 50-100, но это может варьироваться и для более сложных запросов возможно
 * пригодится более большой {@code FetchSize}, чтобы улучшить производительность приложение и не обращаться лишний раз
 * в базу данных. В тоже самое, если {@code FetchSize} будет слишком большой, то в этом случае может просто не хватить
 * памяти (оперативной, т.к. данные хранятся в оперативной) на Java приложение.
 * <br><br>
 * Следует помнить, что в зависимости от драйвера, который мы используем для соединения с базой данных, все настройки
 * по умолчанию будут отличаться.
 */
public class JdbcRunner {
    public static void main(String[] args) {
        // С начала дня 2020-1-01 по сегоднешнее число
        var flightsBetweenResult = getFlightsBetween(
                LocalDate.of(2020, 1, 1).atStartOfDay(),
                LocalDateTime.now()
        );
        System.out.println(flightsBetweenResult);
    }

    /**
     * В каждом запросе, который мы делаем (<b>не</b> соединении), а именно для каждого запроса устанавливаем
     * <i>FetchSize</i> равный двадцати:
     * <pre>{@code prepareStatement.setFetchSize(20);}</pre>
     * Устанавливаем тайм-аут для соединений, измеряется в секундах.
     * <pre>{@code prepareStatement.setQueryTimeout(10);}</pre>
     * Аналог лимита для всех запросов. Т.е., если сделали довольно большую выборку, то setMaxRows обезопасывает нас
     * от того, чтобы у нас не упало Java-приложение, опять же из-за ошибки переполнения памяти.
     * <pre>{@code prepareStatement.setMaxRows(100);}</pre>
     */
    private static List<Long> getFlightsBetween(LocalDateTime start, LocalDateTime end) {
        String sql = """
                SELECT id FROM flight_storage.flight WHERE departure_date BETWEEN ? AND ?
                """;

        List<Long> result = new ArrayList<>();

        try (
                var connection = ConnectionManager.open();
                var prepareStatement = connection.prepareStatement(sql);
        ) {
            prepareStatement.setFetchSize(20);
            prepareStatement.setQueryTimeout(10);
            prepareStatement.setMaxRows(100);

            // Устанавливаем значения
            System.out.println(prepareStatement);
            prepareStatement.setTimestamp(1, Timestamp.valueOf(start));
            System.out.println(prepareStatement);
            prepareStatement.setTimestamp(2, Timestamp.valueOf(end));
            System.out.println(prepareStatement);

            var resultSet = prepareStatement.executeQuery();

            // Сохраняем ID перелётов, пробегаем по resultSet
            while (resultSet.next()) {
                result.add(resultSet.getLong("id"));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    private static List<Long> getTicketsByFlightId(Long flightId) {
        String sql = """
                SELECT id FROM flight_storage.ticket WHERE flight_id = ?
                """;
        List<Long> result = new ArrayList<>();
        try (
                var connection = ConnectionManager.open();
                var prepareStatement = connection.prepareStatement(sql);
        ) {
            prepareStatement.setLong(1, flightId);
            var resultSet = prepareStatement.executeQuery();

            while (resultSet.next()) {
                result.add(resultSet.getObject("id", Long.class));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return result;
    }
}