package util;

import java.io.IOException;
import java.util.Properties;

/**
 * <h1>Класс, умеющий работать с properties файлами</h1>
 */
public final class PropertiesUtil {
    private PropertiesUtil() {

    }

    /**
     * Для представления properties файлов у нас есть специальный класс из java.util.
     * <br><br>
     * Он наследуется от Hashtable. По сути, ассоциативный массив
     * <br><br>
     * Загружаем в создаваемый экземпляр класса наши properties. Для этого создадим статический метод,
     * который вызовем из статического блока инициализации.
     */
    private static final Properties PROPERTIES = new Properties();

    static {
        loadProperties();
    }

    /**
     * Здесь нам нужно считать наш <i>application.properties</i> файл из нашего CLASSPATH.
     * <br><br>
     * Для этого берём из PropertiesUtil класс, getClassLoader и через него загружаем наш файл через
     * getResourceAsStream. Т.е. таким образом мы всегда сможем достучаться до нашего файла, если он
     * лежит в нашем проекте. По сути, в папке src.
     */
    private static void loadProperties() {
        try (var inputStream =
                     PropertiesUtil.
                             class.
                             getClassLoader().
                             getResourceAsStream("application.properties")
        ) {
            PROPERTIES.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Создаём метод, который возвращает значение по ключу из <i>properties</i> файла.
     * По сути, value из нашего "ассоциативного массива".
     * <br><br>
     *
     * @param key
     * @return возвращает значение по ключу из <i>properties</i> файла
     */
    public static String get(String key) {
        return PROPERTIES.getProperty(key);
    }
}
