package com.ekagra.dao;

import java.util.Optional;

/**
 * DAO pattern: generic contract for reading/writing one kind of entity.
 * Services talk to DAOs and never write SQL themselves.
 */
public interface Dao<T> {

    Optional<T> findById(int id);

    /** Saves a new entity and returns its generated id. */
    int insert(T entity);

    void update(T entity);

    void delete(int id);
}
