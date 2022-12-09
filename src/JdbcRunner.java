import util.ConnectionManager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JdbcRunner {
    public static void main(String[] args) {
        checkMetaData();
    }

    private static void checkMetaData() {
        try (var connection = ConnectionManager.open()) {
            // Получаем мета-данные
            var metaData = connection.getMetaData();
            // Получаем все каталоги из мета-данных, на выходе получаем ResultSet, СМОТРЕТЬ ДОКУМЕНТАЦИЮ
            ResultSet catalogs = metaData.getCatalogs();

            // Берём все сущности из схемы flight_storage
            // Проходимся по каталогам
            while (catalogs.next()) {
                // Получаем название каталога
                String catalog = catalogs.getString(1);
                // Получаем схемы каталога
                ResultSet schemas = metaData.getSchemas();

                // Проходимся по схемах каталога
                while (schemas.next()) {
                    // Получаем название схемы каталога
                    String schema = schemas.getString("TABLE_SCHEM");
                    // Получаем все сущности схемы каталога
                    ResultSet tables = metaData.getTables(catalog, schema, "%", null);

                    // Если схема равна нужной нам, то выводим названия таблиц схемы каталога
                    if (schema.equals("flight_storage")) {
                        while (tables.next()) {
                            System.out.println(tables.getString("TABLE_NAME"));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

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