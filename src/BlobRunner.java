import util.ConnectionManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * <h1>Blob & Clob</h1>
 * <ul>
 *     <li>
 *         <b>Blob</b> (binary large object) - большой бинарный объект, проще говоря - массив байтов. Можно добавить
 *         всё, что представляется в виде байтов. Как правило, используется для таких объектов как картинки, видео,
 *         аудио, файлы, ворды, ексель и т.д.
 *     </li>
 *     <li>
 *         <b>Clob</b> - символьный большой объект. Можно добавить всё, что можно представить в виде символов.
 *     </li>
 * </ul>
 * Но не во всех СУБД они присутствуют. В данном случае, в postgres нет blob-a, а есть тип <b>bytea</b> (byte array).
 * Clob-a тоже нет, но есть такой тип данных как <b>TEXT</b>. Ограничения есть и там, и там. Следует обращаться к
 * документации чтобы выяснить, можно ли запихнуть какой-нибудь объект туда.
 * <br><br>
 * Создадим в базе данных, в таблице aircraft поле, которое будет из себя представлять массив байтов
 * ({@code alterColumnToAircraft();}).
 * <br><br>
 * Напишем код для считывания картинки и сохранения в базу данных.
 * <h2>Примечание: код здесь, на JDK 19 не работает, хотя у первоисточника работает.</h2>
 * По-хорошему, не следует большие объекты ложить в базу данных. Это гораздо замедляет работу всех баз данных и все
 * запросы. Как правило, большие объекты (картинки, видео и т.д.) хранятся в сторонних хранилищах, а в базе данных
 * только ссылка на них.
 */
public class BlobRunner {
    public static void main(String[] args) {
        // alterColumnToAircraft();
        // saveImage();
        getImage();
    }

    /**
     * <h1>Метод для считывания картинки и сохранения в базу данных.</h1>
     * <b>Т.к. картинка это большой объект, следует открывать и закрывать транзакцию</b>
     * <br><br>
     * Этот код <b>не</b> будет работать, т.к. в postgres нет поддержки блобов и клобов:
     * <pre>{@code
     *             connection.setAutoCommit(false);
     *
     *             // Создаём блоб
     *             Blob blob = connection.createBlob();
     *             // Устанавливаем байты картинки с первой позиции
     *             blob.setBytes(1, Files.readAllBytes(Path.of("resources", "Boeing777.jpg")));
     *             // В стейтмент передаём блоб
     *             preparedStatement.setBlob(1, blob);
     *             preparedStatement.executeUpdate();
     *
     *             // Примечание - здесь реализация ручной транзакция сделана упрощённо
     *             connection.commit();
     * }</pre>
     * <b>Перейдём к реализации задачи на postgres, без поддержки интерфейсов блобов и клобов.</b> Здесь нам нужны
     * массивы байт. Поэтому у нас есть специально метод {@code .setBytes();}:
     * <pre>{@code
     *             preparedStatement.setBytes(1, Files.readAllBytes(Path.of("resources", "Boeing777.jpg")));
     *             preparedStatement.executeUpdate();
     * }</pre>
     */
    private static void saveImage() {
        String sql = """
                UPDATE flight_storage.aircraft SET image = ? WHERE id = 1
                """;
        try (
                Connection connection = ConnectionManager.open();
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
        ) {
            byte[] image = Files.readAllBytes(Path.of("resources", "Boeing777.jpg"));
            preparedStatement.setBytes(1, image);
            preparedStatement.executeUpdate();
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * <h1>Метод для получения изображения</h1>
     */
    private static void getImage() {
        String sql = """
                SELECT image FROM flight_storage.aircraft WHERE id = ?;
                """;
        try (
                Connection connection = ConnectionManager.open();
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
        ) {
            preparedStatement.setInt(1, 1);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                byte[] image = resultSet.getBytes("image");
                Files.write(Path.of("resources", "Boeing777_new.jpg"), image, StandardOpenOption.CREATE);
            }
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void alterColumnToAircraft() {
        String sql = "ALTER TABLE flight_storage.aircraft ADD image BYTEA;";
        try (
                Connection connection = ConnectionManager.open();
                PreparedStatement preparedStatement = connection.prepareStatement(sql)
        ) {
            preparedStatement.execute();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
