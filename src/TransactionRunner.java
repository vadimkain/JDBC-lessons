import util.ConnectionManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * <h1>Batch запросы</h1>
 * Представим такую ситуацию: у нас есть четыре разных запроса:
 * <ol>
 *     <li>Вставляем какую-ту запись в таблицу airport</li>
 *     <li>Вставляем запись в таблицу flight</li>
 *     <li>Вставляем запись в таблицу ticket</li>
 *     <li>Обновляем departure_date поле в таблице flight</li>
 * </ol>
 * <img src="../Batch1.png" />
 * <img src="../Batch2.png" />
 * <img src="../Batch3.png" />
 * <br>
 * Т.е. по сути, выполнилось четыре разных запросов, четыре разных транзакций. Более того, мы четыре раза сходили в БД
 * и на это ушло определённое время. Как сходить один раз на сервер базы данных, где бы он не находился и сразу же
 * выполнить все четыре запроса? По сути, мы бы открыли одну транзакцию, отправили все четыре запроса, которые хотим
 * выполнить и вернулись с ними. Для этого как раз таки используются <b>Batch-запросы</b>.
 * <br><br>
 * Так мы ходим в базу данных два раза:
 * <pre>{@code
 *             int deletedTicketsResult = deleteTicketsStatement.executeUpdate();
 *             if (true) {
 *                 throw new RuntimeException("Ooops");
 *             }
 *             int deleteFlightResult = deleteFlightStatement.executeUpdate();
 * }</pre>
 * Для того чтобы не ходить два раза в базу данных, нам надо использовать не
 * {@code connection.prepareStatement();}, а {@code connection.statement();}. Только он позволяет использовать
 * <i>Batch</i> запросы.
 * <br><br>
 * После того, как открыли соединение и создали statement ({@code statement = connection.createStatement();} ) вызываем
 * метод {@code .addBatch(sql)} и добавляем какой-нибудь SQL запрос:
 * <pre>{@code
 *             statement = connection.createStatement();
 *             statement.addBatch(deleteTicketsSql);
 *             statement.addBatch(deleteFlightSql);
 * }</pre>
 * Мы не можем выполнять {@code .addBatch(sql)} с {@code PreparedStatement} потому что он при создании сразу вызывает
 * SQL запрос.
 * <br><br>
 * В тоже время, теперь нам не подходит работа с SQL запросами через вопросительный знак, потому что должны сами
 * добавлять в конце наши <i>flight_id</i>:
 * <pre>{@code
 *         var deleteFlightSql = "DELETE FROM flight_storage.flight WHERE id = " + flightId;
 *         var deleteTicketsSql = "DELETE FROM flight_storage.ticket WHERE id = " + flightId;
 * }</pre>
 * И выполняем их батчем и он выполняет все наши батч-запросы:
 * <pre>{@code int[] ints = statement.executeBatch();}</pre>
 * Результатом выполнения возвращается массив обновлённых значений. Т.е. для каждого выполненного запроса возвращается
 * результат его выполнения. <b>Батчи используют в основном для операций DDL.</b>
 * <br><br>
 * Т.к. всё выполняется в рамках одной транзакции, - либо выполнятся все запросы, либо ни одной.
 * <br><br>
 * Тут поведение такое же, как и у обычных транзакций, только в данном случае экономим время на пересылку каждого
 * из этих запросов, потому что посылаем их батчем, т.е. сразу всем скоупом.
 * <br><br>
 * <b>Batch очень хорошая вещь, если необходимо сэкономить время на отправки запрсов на сторону сервера базы данных.</b>
 */
public class TransactionRunner {
    public static void main(String[] args) throws SQLException {
        long flightId = 8;

        var deleteFlightSql = "DELETE FROM flight_storage.flight WHERE id = " + flightId;
        var deleteTicketsSql = "DELETE FROM flight_storage.ticket WHERE id = " + flightId;

        Connection connection = null;
        Statement statement = null;

        try {
            connection = ConnectionManager.get();
            connection.setAutoCommit(false);

            statement = connection.createStatement();

            statement.addBatch(deleteTicketsSql);
            statement.addBatch(deleteFlightSql);

            int[] ints = statement.executeBatch();

            connection.commit();
        } catch (Exception e) {
            // connection может быть неинициализирован, поэтому проверяем на null
            if (connection != null) {
                connection.rollback();
            }
            throw e;
        } finally {
            // Закрываем соединение
            if (connection != null) {
                connection.close();
            }
        }
    }
}
