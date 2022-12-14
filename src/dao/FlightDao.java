package dao;

import entity.Flight;
import exception.DaoException;
import util.ConnectionManager;

import javax.xml.transform.Result;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * <b>Обычно для DAO создают интерфейс, где есть все CRUD операции и просто в каждом из DAO его реализуют</b>
 */
public class FlightDao implements Dao<Long, Flight> {
    private static String FIND_BY_ID_SQL = """
            SELECT 
            id,
            status, 
            aircraft_id, 
            arrival_airport_code, 
            arrival_date, 
            departure_airport_code, 
            flight_no,
            departure_date
            FROM flight_storage.flight WHERE id = ?
            """;

    @Override
    public boolean delete(Long key) {
        return false;
    }

    @Override
    public Flight save(Flight entity) {
        return null;
    }

    @Override
    public void update(Flight entity) {

    }

    @Override
    public List<Flight> findAll() {
        return null;
    }

    /**
     * {@code .findById(Long key);} требует нового соединения, а такого делать не стоит, потому что во-первых у нас
     * есть connection pool и он ограничен в размерах, следовательно, он может просто закончиться или долго ожидать
     * до тех пор, пока кто-то другой из запросов не вернёт в этот пул, либо что ещё хуже, может быть dead lock,
     * если кто-то одновременно вызывает {@code .findById()} у ticket-а и далее исчерпали все наши соединения и ожидаем
     * до тех пор, пока кто-то не вернёт в наш connection pool для того чтобы выполнить {@code .findById();} у
     * нашего flight-a, что естественно опасно. Следовательно, в реальных приложениях connection открывают на уровне
     * сервисов и уже в сервисах просто передают эти соединения на уровень DAO, естественно там делают более гибкий
     * вариант с помощью АОП, либо можем реализовать с помощью thread local перменных, либо самый простой вариант - это
     * создать {@code .findById(Long key)}, который принимает ещё и соединение:
     * <pre>{@code public Optional<Flight> findById(Long key, Connection connection)}</pre>
     * Выходит, если нам нужен ещё один connection, то мы его не открываем, а просто выполняем с помощью существующего,
     * а главное, мы его не закрываем.
     * @param key
     * @param connection
     * @return
     */
    public Optional<Flight> findById(Long key, Connection connection) {
        try (
                PreparedStatement preparedStatement = connection.prepareStatement(FIND_BY_ID_SQL);
        ) {
            preparedStatement.setLong(1, key);

            ResultSet resultSet = preparedStatement.executeQuery();

            Flight flight = null;
            if (resultSet.next()) {
                flight = new Flight(
                        resultSet.getLong("id"),
                        resultSet.getString("flight_no"),
                        resultSet.getTimestamp("departure_date").toLocalDateTime(),
                        resultSet.getString("departure_airport_code"),
                        resultSet.getTimestamp("arrival_date").toLocalDateTime(),
                        resultSet.getString("arrival_airport_code"),
                        resultSet.getInt("aircraft_id"),
                        resultSet.getString("status")
                );
            }

            return Optional.ofNullable(flight);
        } catch (SQLException throwables) {
            throw new DaoException(throwables);
        }

    }

    @Override
    public Optional<Flight> findById(Long key) {
        try (
                Connection connection = ConnectionManager.get();
        ) {
            return findById(key, connection);
        } catch (SQLException throwables) {
            throw new DaoException(throwables);
        }
    }

    private static final FlightDao INSTANCE = new FlightDao();

    private FlightDao() {
    }

    public static FlightDao getInstance() {
        return INSTANCE;
    }
}
