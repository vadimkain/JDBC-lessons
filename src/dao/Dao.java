package dao;

import java.util.List;
import java.util.Optional;

// K - ключ, E - сущность
public interface Dao<K, E> {
    boolean delete(K key);

    E save(E entity);

    void update(E entity);

    List<E> findAll();

    Optional<E> findById(K key);
}
