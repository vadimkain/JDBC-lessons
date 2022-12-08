import util.ConnectionManager;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * <h1>PreparedStatement</h1>
 * Это интерфейс, который наследуется от {@code Statement}, следовательно, он может всё тоже самое, что и
 * {@code Statement}. В этом интерфейсе много сеттеров в отличии от родителя.
 * <br><br>
 * Перепишем код, но при помощи {@code PreparedStatement}. Для этого создаём не
 * <pre>{@code var statement = connection.createStatement();}</pre>
 * ,а
 * <pre>{@code var prepareStatement = connection.prepareStatement(sql);}</pre>
 * У него есть много перегруженных методов, но суть их такова, что все они принимают первым параметром sql и затем,
 * мы уже не должны передавать sql во все {@code execute()} методы.
 * <br><br>
 * Далее, суть {@code preparedStatement(sql)} состоит в том, что все параметры мы не форматируем, а все неизвестные
 * параметры мы перечисляем через знак вопроса:
 * <pre>{@code
 *         String sql = """
 *                 SELECT id FROM flight_storage.ticket WHERE flight_id = ?
 *                 """;
 * }</pre>
 * Те методы {@code set...();}, которые у интерфейса {@code PrepatedStatement} и нужны для того чтобы устанавливать
 * эти значения в {@code ?}. Их нужно установить до того, как вызвали {@code prepareStatement.executeQuery()}:
 * <pre>{@code prepareStatement.setLong(1, flightId);}</pre>
 * Первый аргументом является номер вопросительного знака, а вторым параметром - его значение. Т.к. работаем с
 * примитивными типами данных, нужно быть осторожным и если наш параметр, который пришёл в наш метод может быть
 * {@code null}, тогда лучше воспользоваться {@code setObject()}, хотя в таком случае у нас тогда запрос будет невалидным,
 * потому что на {@code null} все sql операции используют isNull или isNotNull не равно.
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
     * Для {@code PreparedStatement} могут выполняться любые запросы. <b>Более того,</b> {@code PreparedStatement}
     * очень хорошо работает с разными типами данных, он сам знает, где нужно поставить одинарные кавычки для строк,
     * либо есди работаем с датами, то знает как преобразовать дату в нужную строку, которая понимает нашу базу
     * данных.
     * <h2>Найдём перелёты между двумя датами</h2>
     *
     * @param start начальная дата
     * @param end   конечная дата
     * @return возвращает все наши id перелётов
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