package com.placementpro.backend.repository;

import com.placementpro.backend.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query("""
            SELECT u FROM User u
            LEFT JOIN FETCH u.roles r
            LEFT JOIN FETCH u.studentProfile sp
            LEFT JOIN FETCH u.employerProfile ep
            LEFT JOIN FETCH u.placementProfile pp
            WHERE LOWER(u.email) = LOWER(:email)
            """)
    Optional<User> findDetailedByEmail(@Param("email") String email);

    @Query("""
            SELECT u FROM User u
            LEFT JOIN FETCH u.roles r
            LEFT JOIN FETCH u.studentProfile sp
            LEFT JOIN FETCH u.employerProfile ep
            LEFT JOIN FETCH u.placementProfile pp
            WHERE u.id = :id
            """)
    Optional<User> findDetailedById(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT u FROM User u
            LEFT JOIN FETCH u.roles r
            LEFT JOIN FETCH u.studentProfile sp
            LEFT JOIN FETCH u.employerProfile ep
            LEFT JOIN FETCH u.placementProfile pp
            """)
    List<User> findAllDetailed();

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRoles_Name(String name);
    long countByRoles_Name(String name);
}
