package dao;

import entity.Ticket;
import exception.DaoException;
import util.ConnectionManager;

import java.sql.*;

/**
 * <h1>DAO. Операции DELETE и INSERT</h1>
 * Основные операции, которые можем делать с таблицей:
 * <ul>
 *     <li>Create</li>
 *     <li>Read</li>
 *     <li>Update</li>
 *     <li>Delete</li>
 * </ul>
 * И в основном все эти четыре операции есть в каждом из DAO. Следовательно, мы должны реализовать его и в нашем
 * TicketDao.
 */
public class TicketDao {
    private static final TicketDao INSTANCE = new TicketDao();

    private static final String DELETE_SQL = """
            DELETE FROM flight_storage.ticket WHERE id = ?
            """;
    private static final String SAVE_SQL = """
            INSERT INTO flight_storage.ticket (passenger_no, passenger_name, flight_id, seat_no, cost)
            VALUES (?, ?, ?, ?, ?);
            """;

    private TicketDao() {
    }

    public static TicketDao getInstance() {
        return INSTANCE;
    }

    public Ticket save(Ticket ticket) {
        try (
                Connection connection = ConnectionManager.get();
                PreparedStatement preparedStatement = connection.prepareStatement(
                        SAVE_SQL,
                        Statement.RETURN_GENERATED_KEYS
                );
        ) {
            preparedStatement.setString(1, ticket.getPassengerNo());
            preparedStatement.setString(2, ticket.getPassengerName());
            preparedStatement.setLong(3, ticket.getFlightId());
            preparedStatement.setString(4, ticket.getSeatNo());
            preparedStatement.setBigDecimal(5, ticket.getCost());

            preparedStatement.executeUpdate();

            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();

            if (generatedKeys.next()) {
                ticket.setId(generatedKeys.getLong("id"));
            }

            return ticket;

        } catch (SQLException throwables) {
            throw new DaoException(throwables);
        }
    }

    public boolean delete(Long id) {
        try (
                Connection connection = ConnectionManager.get();
                PreparedStatement preparedStatement = connection.prepareStatement(DELETE_SQL);
        ) {
            preparedStatement.setLong(1, id);
            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException throwables) {
            // Правила хорошего тона гласят, что лучше создавать свой Exception
            throw new DaoException(throwables);
        }
    }

}
