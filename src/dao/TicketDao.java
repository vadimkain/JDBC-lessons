package dao;

import entity.Ticket;
import exception.DaoException;
import util.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    private static final String DELETE_SQL = """
            DELETE FROM flight_storage.ticket WHERE id = ?
            """;
    private static final String SAVE_SQL = """
            INSERT INTO flight_storage.ticket (passenger_no, passenger_name, flight_id, seat_no, cost)
            VALUES (?, ?, ?, ?, ?);
            """;
    public static final String UPDATE_SQL = """
            UPDATE flight_storage.ticket 
            SET passenger_no = ?, passenger_name = ?, flight_id = ?, seat_no = ?, cost = ?
            WHERE id = ?
            """;
    private static final String FIND_ALL_SQL = """
            SELECT id, passenger_no, passenger_name, flight_id, seat_no, cost
            FROM flight_storage.ticket
            """;
    public static final String FIND_BY_ID_SQL = FIND_ALL_SQL + """
             WHERE id = ?
            """;

    public List<Ticket> findAll() {
        try (
                Connection connection = ConnectionManager.get();
                PreparedStatement preparedStatement = connection.prepareStatement(FIND_ALL_SQL);
        ) {
            ResultSet resultSet = preparedStatement.executeQuery();

            List<Ticket> tickets = new ArrayList<>();

            while (resultSet.next()) {
                tickets.add(buildTicket(resultSet));
            }

            return tickets;
        } catch (SQLException throwables) {
            throw new DaoException(throwables);
        }
    }

    /**
     * <b>Если метод может вернуть {@code null}, то мы должны возвращать {@code Optional<>}. В случае
     * с коллекциями, мы возвращаем пустую коллекцию. <i>Это правило хорошего тона</i>.</b>
     */
    public Optional<Ticket> findById(Long id) {
        try (
                Connection connection = ConnectionManager.get();
                PreparedStatement preparedStatement = connection.prepareStatement(FIND_BY_ID_SQL);
        ) {
            preparedStatement.setLong(1, id);

            ResultSet resultSet = preparedStatement.executeQuery();

            Ticket ticket = null;

            if (resultSet.next()) {
                ticket = buildTicket(resultSet);
            }

            return Optional.ofNullable(ticket);
        } catch (SQLException throwables) {
            throw new DaoException(throwables);
        }
    }

    private static Ticket buildTicket(ResultSet resultSet) throws SQLException {
        return new Ticket(
                resultSet.getLong("id"),
                resultSet.getString("passenger_no"),
                resultSet.getString("passenger_name"),
                resultSet.getLong("flight_id"),
                resultSet.getString("seat_no"),
                resultSet.getBigDecimal("cost")
        );
    }

    public void update(Ticket ticket) {
        try (
                Connection connection = ConnectionManager.get();
                PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_SQL);
        ) {
            preparedStatement.setString(1, ticket.getPassengerNo());
            preparedStatement.setString(2, ticket.getPassengerName());
            preparedStatement.setLong(3, ticket.getFlightId());
            preparedStatement.setString(4, ticket.getSeatNo());
            preparedStatement.setBigDecimal(5, ticket.getCost());
            preparedStatement.setLong(6, ticket.getId());

            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            throw new DaoException(e);
        }
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


    private static final TicketDao INSTANCE = new TicketDao();

    private TicketDao() {
    }

    public static TicketDao getInstance() {
        return INSTANCE;
    }
}
