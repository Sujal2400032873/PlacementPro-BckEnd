package com.placementpro.backend.repository;

import com.placementpro.backend.entity.Application;
import com.placementpro.backend.entity.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    @Query("""
            SELECT a FROM Application a
            JOIN FETCH a.job j
            JOIN FETCH a.student s
            LEFT JOIN FETCH s.studentProfile sp
            WHERE s.id = :studentId
            ORDER BY a.appliedAt DESC
            """)
    List<Application> findDetailedByStudentId(@Param("studentId") Long studentId);

    @Query("""
            SELECT a FROM Application a
            JOIN FETCH a.job j
            JOIN FETCH a.student s
            LEFT JOIN FETCH s.studentProfile sp
            WHERE j.id = :jobId
            ORDER BY a.appliedAt DESC
            """)
    List<Application> findByJobId(@Param("jobId") Long jobId);

    @Query("""
            SELECT a FROM Application a
            JOIN FETCH a.job j
            JOIN FETCH a.student s
            LEFT JOIN FETCH s.studentProfile sp
            WHERE j.employer.id = :employerId
            ORDER BY a.appliedAt DESC
            """)
    List<Application> findDetailedByEmployerId(@Param("employerId") Long employerId);

    @Query("""
            SELECT a FROM Application a
            JOIN FETCH a.student s
            JOIN FETCH a.job j
            LEFT JOIN FETCH s.studentProfile sp
            WHERE j.employer.id = :employerId
            ORDER BY a.appliedAt DESC
            """)
    List<Application> findByEmployerId(@Param("employerId") Long employerId);

        void deleteByStudent_Id(Long studentId);

        void deleteByJob_Id(Long jobId);

        @EntityGraph(attributePaths = {"job", "student", "student.studentProfile"})
        Page<Application> findByStudent_IdOrderByAppliedAtDesc(Long studentId, Pageable pageable);

        @EntityGraph(attributePaths = {"job", "student", "student.studentProfile"})
        Page<Application> findByJob_IdOrderByAppliedAtDesc(Long jobId, Pageable pageable);

        @EntityGraph(attributePaths = {"job", "student", "student.studentProfile"})
        Page<Application> findByJob_Employer_IdOrderByAppliedAtDesc(Long employerId, Pageable pageable);

    @Query("""
            SELECT a FROM Application a
            JOIN FETCH a.job j
            JOIN FETCH a.student s
            LEFT JOIN FETCH s.studentProfile sp
            WHERE a.id = :id
            """)
    java.util.Optional<Application> findDetailedById(@Param("id") Long id);

    boolean existsByJob_IdAndStudent_Id(Long jobId, Long studentId);

    long countByStatus(ApplicationStatus status);

    @Query("""
            SELECT COUNT(DISTINCT a.student.id) FROM Application a
            WHERE a.status IN :statuses
            """)
    long countDistinctStudentsByStatuses(@Param("statuses") List<com.placementpro.backend.entity.ApplicationStatus> statuses);
}
