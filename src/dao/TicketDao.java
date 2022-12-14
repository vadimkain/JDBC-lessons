package dao;

import dto.TicketFilter;
import entity.Ticket;
import exception.DaoException;
import util.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

/**
 * <h1>DAO. Batch SELECT с фильтрацией</h1>
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

    /**
     * SQL запрос {@code .prepareStatement()} у нас динамический, основанием которого служет {@code FIND_ALL_SQL}.
     * Но далее на основании фильтра мы должны достроить условие <i>WHERE</i>, если есть такие поля и добавить
     * <i>LIMIT & OFFSET</i> в конце. Поэтому, этот SQL мы будем формировать во время выполнения нашего метода
     * {@code .findAll(TicketFilter filter)}.
     * <br><br>
     * Создаём коллекцию параметров, передавая в дженерики класс Object, потому-что у нас могут быть разные запросы
     * и разное количество параметров:
     * <pre>{@code
     *         List<Object> parameters = new ArrayList<>();
     *         List<String> whereSql = new ArrayList<>();
     *
     *         if (filter.seatNo() != null) {
     *             whereSql.add("seat_no LIKE ?");
     *             // % для LIKE оператора
     *             parameters.add("%" + filter.seatNo() + "%");
     *         }
     *         if (filter.passengerName() != null) {
     *             whereSql.add("passenge_name = ?");
     *             parameters.add(filter.passengerName());
     *         }
     *         parameters.add(filter.limit());
     *         parameters.add(filter.offset());
     * }</pre>
     * Далее нам надо пройтись циклом по этим параметрам и установить их в {@code .prepareStatement()}:
     * <pre>{@code
     *             for (int i = 0; i < parameters.size(); i++) {
     *                 preparedStatement.setObject(i + 1, parameters.get(i));
     *             }
     * }</pre>
     * Если в фильтре не будет ни одного параметра, то нужно добавить пустую строку вместо WHERE (иначе будет ошибка)
     *
     * @param filter
     * @return
     */
    public List<Ticket> findAll(TicketFilter filter) {
        List<Object> parameters = new ArrayList<>();
        List<String> whereSql = new ArrayList<>();

        if (filter.seatNo() != null) {
            whereSql.add("seat_no LIKE ?");
            // % для LIKE оператора
            parameters.add("%" + filter.seatNo() + "%");
        }
        if (filter.passengerName() != null) {
            whereSql.add("passenger_name = ?");
            parameters.add(filter.passengerName());
        }
        parameters.add(filter.limit());
        parameters.add(filter.offset());

        // Stream API
        // ВСЕГДА используем статический импорт для коллекторов
        String where = whereSql.stream()
                .collect(joining(" AND ", " WHERE ", " LIMIT ? OFFSET ? "));

        String sql = FIND_ALL_SQL + where;

        try (
                Connection connection = ConnectionManager.get();
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
        ) {
            for (int i = 0; i < parameters.size(); i++) {
                preparedStatement.setObject(i + 1, parameters.get(i));
            }
            System.out.println(preparedStatement);

            ResultSet resultSet = preparedStatement.executeQuery();

            // Результирующий набор тикетов
            List<Ticket> tickets = new ArrayList<>();
            while (resultSet.next()) {
                tickets.add(buildTicket(resultSet));
            }

            return tickets;
        } catch (SQLException throwables) {
            throw new DaoException(throwables);
        }
    }

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
