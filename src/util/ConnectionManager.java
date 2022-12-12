package util;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * <h1>Connection pool (Пул соединений)</h1>
 * В современных приложениях <b>всегда</b> используется connection pool и никогда не создаются новые соединения, когда
 * необходимо выполнить любой запрос из стороны Java-приложения. По сути, connection pool представляет из себя
 * обычную структуру данных, какую-либо java коллекцию, например очередь или список, которая сразу же инициализирует
 * несколько соединений. Например:
 * <br><br>
 * <img src="ConnectionPool1.png" />
 * <br>
 * В данном примере проинициализировали очередь из пяти соединений. В данном случае размер равняется пяти. Далее, мы
 * больше никогда не создаём соединения нашего приложения. Поэтому, теперь, когда мы пишем
 * <pre>{@code try (Connection connection = ConnectionManager.open())}</pre>- мы не будем создавать новое соединение,
 * а возвращаем уже готовое проинициализированное соединение из нашей коллекции, для того чтобы выполнить какой-либо
 * запрос из потока. Естественно предположить, что мы не можем одновременно выполнять более пяти запросов, потому
 * что пул соединений равен пяти. Но на самом деле этого и не нужно, потому что в реальных приложениях достаточно
 * иметь пул соединений в размере от пяти до двадцати, не больше, это более чем достаточно для дееспособности нашего
 * сервиса. Поэтому, когда берём наше соединение для того, чтобы выполнить запрос - мы получаем из нашего пула готовое
 * соединение, выполняем наш код и как только выходим из него (по сути, должны закрыть соеиднение), то вместо закрытия
 * просто возвращаем в наш пул соединение после использования.
 * <br><br>
 * <h2>Перепишем {@code class ConnectionManager} таким образом.</h2>
 * На реальной практике таких классов не создают, потому что уже есть готовые библиотеки.
 * <br>
 * В <b>application.properties</b> указываем {@code db.pool.size=5}.
 */
public final class ConnectionManager {

    private static final String URL_KEY = "db.url";
    private static final String USERNAME_KEY = "db.username";
    private static final String PASSWORD_KEY = "db.password";
    private static final String POOL_SIZE_KEY = "db.pool.size";
    private static final int DEFAULT_POOL_SIZE = 10;

    // Объявляем потокобезопасною очередь
    private static BlockingQueue<Connection> pool;
    // Объявляем список для закрытия соединений. Здесь храним исходные соединения
    private static List<Connection> sourceConnections;

    static {
        loadDriver();
        initConnectionPool();
    }

    private ConnectionManager() {

    }

    /**
     * <h1>Метод, который инициализирует пул соединений</h1>
     */
    private static void initConnectionPool() {
        // Получаем размер пула
        String poolSize = PropertiesUtil.get(POOL_SIZE_KEY);
        // Устанавливаем размер
        int size = poolSize == null ? DEFAULT_POOL_SIZE : Integer.parseInt(poolSize);
        // Инициализируем массив потокобезопасной очереди
        pool = new ArrayBlockingQueue<>(size);

        sourceConnections = new ArrayList<>(size);

        // Проходимся по пулу и вставляем туда все наши соединения
        for (int i = 0; i < size; i++) {
            Connection connection = open();
            // Reflection API
            Connection proxyConnection = (Connection) Proxy.newProxyInstance(
                    ConnectionManager.class.getClassLoader(),
                    new Class[]{Connection.class},
                    ((proxy, method, args) -> method.getName().equals("close")
                            ? pool.add((Connection) proxy)
                            : method.invoke(connection, args)
                    )
            );
            pool.add(proxyConnection);
            sourceConnections.add(connection);
        }
    }

    /**
     * <h1>Открытый метод, который достаёт соединения из нашего пулла</h1>
     *
     * @return Возвращаем соединение, если оно есть. Если пул пустой, тогда ждёт.
     */
    public static Connection get() {
        try {
            return pool.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static Connection open() {
        // Создаём соединение
        try {
            return DriverManager.getConnection(
                    PropertiesUtil.get(URL_KEY),
                    PropertiesUtil.get(USERNAME_KEY),
                    PropertiesUtil.get(PASSWORD_KEY)
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * <h1>Метод для закрытия соединения в пулле</h1>
     * Здесь мы должны пройтись по каждому соединению и закрыть его, но вызов метода {@code .close()} у {@code Proxy}
     * возвращает в пул, а не закрывает его. Поэтому, нам нужна ещё одна коллекция, где мы будем хранить исходные
     * соединения, которые нужны будут для закртия в конце:
     * <pre>{@code
     *     // Объявляем список для закрытия соединений
     *     private static List<Connection> sourceConnections;
     * }</pre>
     */
    public static void closePool() {
        try {
            for (Connection sourceConnection : sourceConnections) {
                sourceConnection.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void loadDriver() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
