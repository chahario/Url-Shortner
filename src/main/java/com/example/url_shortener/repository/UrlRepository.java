package com.example.url_shortener.repository;


import com.example.url_shortener.domain.Url;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface UrlRepository extends JpaRepository<Url, String>{
    // it takes to parameters 1: the entity on which it was acting , 2) the Id type of that
    // entity(it was a String).

    @Query("SELECT u FROM Url u WHERE u.clientId = :clientId " +
            "AND (:cursor IS NULL OR u.createdAt < :cursor) " +
            "ORDER BY u.createdAt DESC")
    List<Url> findPage(@Param("clientId") Long clientId,
                       @Param("cursor") Instant cursor,
                       Pageable pageable);
}
