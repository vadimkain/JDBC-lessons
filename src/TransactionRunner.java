import util.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * <h1>Транзакции и блокировки</h1>
 * Транзакция - это единица работы в рамках соединения с базой данных. Другими словами говоря, если мы хотим выполнить
 * совершенно любой sql запрос в нашу СУБД, то нам необходимо открыть транзакцию, выполнить этот запрос и закрыть её.
 * Но если у нас появляется какая-то ошибка во время выполнения, то мы должны сделать rollback, следовательно, у
 * транзакции есть только два состояния - либо <b>commit</b>, когда она (транзакция) полностью выполняется, либо
 * <b>rollback</b>, когда полностью всё откатывается. Поэтому транзакция атомарны - либо выполняются все запросы в
 * рамках текущей транзакции, либо не выполняются ни одного.
 * <br><br>
 * <img src="../Транзакции1.png" />
 * <h1>Это класс, в котором выполняем транзакции</h1>
 */
public class TransactionRunner {
    /**
     * <b>Напишем запрос, который будет удалять перелёты по его id</b>
     * <br><br>
     * У нас вылезет ошибка, потому что в таблице flight есть поле, которое ссылается на ticket. Как правило,
     * для таких ситуаций указывается CASCADE ON DELETE (при удалении родительской записи - все строки, которые
     * ссылаются на таблицу будут удалены автоматически), но в нашем случае это не подходит, потому что не добавили
     * такое удаление. Следовательно, нам нужно сделать второй запрос, который удалит все записи из таблицы ticket,
     * перед тем как удалять все записи из таблицы flight. Поэтому напишем второй запрос:
     * <pre>{@code var deleteTicketsSql = "DELETE FROM flight_storage.ticket WHERE id = ?";}</pre>
     * Представим, что у нас есть какая-то исключительная ситуация, т.е. во время выполнения нашего запроса на удаление
     * ticket-ов все прошло успешно, но во время выполнения удаления flight произошел какой-нибудь Exception,
     * следовательно, все наши ticket-ы будут удалены, а flight, который в принципе и хотели удалить - останется,
     * потому что все запросы по умолчанию выполняются в рамках транзакций. Если сделаем что=то вроде такого:
     * <pre>{@code
     *             deleteTicketsStatement.executeUpdate();
     *             if (true) {
     *                 throw new RuntimeException("Ooops");
     *             }
     *             deleteFlightStatement.executeUpdate();
     * }</pre>
     * , в таком случае у нас 100% удалятся ticket-ы, но не удалятся flight, чего мы <b>не хотим</b> делать.
     * Следовательно, нам надо как-то исправить эту ситуацию и удалить всё сразу, либо не удалять ничего. Это и есть
     * атомарность, которая предоставляется транзакциями.
     * <br><br>
     * Чтобы это сделать, для начала нам надо убрать AUTO_COMMIT_MODE, т.е. чтобы каждый из наших запросов не выполнялся
     * автоматически, мы должны у нашего соединения вызвать метод {@code connection.setAutoCommit(false);} и установить
     * {@code false}, потому что по дефолту он {@code true}. <b>Делать это надо в самом начале, до выполнения любых
     * запросов!</b> Таким образом, мы берём управление транзакциями на себя.
     * <br><br>
     * Теперь, выполняя все наши запросы {@code executeUpdate()} для нашего удаления ticket-ов и flight-ов, мы после
     * всех запросов должны делать коммит: {@code connection.commit();}, т.е. фиксирование транзакций. <b>Нельзя
     * вызвать методы {@code commit()} и {@code rollback()} если стоит {@code AutoCommit(true)} - будет исключение.</b>
     * <br><br>
     * Таким образом, теперь только после вызова {@code connection.commit()} у нас произойдет фиксирование нашей
     * транзакции и удалятся все записи из соответствующих таблиц, но в случае исключения мы должны отловить его и
     * вызвать у того же соединения метод {@code rollback()}.
     * <br><br>
     * <b>Таким образом, то что мы написали</b> - мы во время удаления наших ticket-ов
     * {@code deleteTicketsStatement.executeUpdate();} и потом последующим пробрасыванием какого-то Exception-а:
     * <pre>{@code
     *             if (true) {
     *                 throw new RuntimeException("Ooops");
     *             }
     * }</pre>
     * откатим всю нашу транзакцию:
     * <pre>{@code
     *         catch (Exception e) {
     *             // connection может быть неинициализирован, поэтому проверяем на null
     *             if (connection != null) {
     *                 connection.rollback();
     *             }
     *             throw e;
     *         }
     * }</pre>
     * и не произойдёт фиксирование {@code connection.commit();}.
     */
    public static void main(String[] args) throws SQLException {
        long flightId = 8;

        var deleteFlightSql = "DELETE FROM flight_storage.flight WHERE id = ?";
        var deleteTicketsSql = "DELETE FROM flight_storage.ticket WHERE id = ?";

        Connection connection = null;
        PreparedStatement deleteFlightStatement = null;
        PreparedStatement deleteTicketsStatement = null;

        try {
            connection = ConnectionManager.open();
            deleteFlightStatement = connection.prepareStatement(deleteFlightSql);
            deleteTicketsStatement = connection.prepareStatement(deleteTicketsSql);

            connection.setAutoCommit(false);

            deleteFlightStatement.setLong(1, flightId);
            deleteTicketsStatement.setLong(1, flightId);

            deleteTicketsStatement.executeUpdate();
            if (true) {
                throw new RuntimeException("Ooops");
            }
            deleteFlightStatement.executeUpdate();

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
            if (deleteFlightStatement != null) {
                deleteFlightStatement.close();
            }
            if (deleteTicketsStatement != null) {
                deleteFlightStatement.close();
            }
        }
    }
}
