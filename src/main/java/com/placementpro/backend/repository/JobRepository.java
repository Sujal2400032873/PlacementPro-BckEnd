package com.placementpro.backend.repository;

import com.placementpro.backend.entity.Job;
import com.placementpro.backend.entity.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface JobRepository extends JpaRepository<Job, Long> {
    @Query("""
            SELECT j FROM Job j
            LEFT JOIN FETCH j.employer e
            WHERE j.status = :status
            ORDER BY j.postedAt DESC
            """)
    List<Job> findDetailedByStatus(@Param("status") JobStatus status);

    @Query("""
            SELECT j FROM Job j
            LEFT JOIN FETCH j.employer e
            WHERE e.id = :employerId
            ORDER BY j.postedAt DESC
            """)
    List<Job> findDetailedByEmployerId(@Param("employerId") Long employerId);

    @Query("""
            SELECT j FROM Job j
            LEFT JOIN FETCH j.employer e
            WHERE j.id = :id
            """)
    Optional<Job> findDetailedById(@Param("id") Long id);

    @Query("SELECT j FROM Job j WHERE j.title LIKE %?1% OR j.companyName LIKE %?1%")
    List<Job> searchJobs(String keyword);

    @Query("""
            SELECT j FROM Job j
            LEFT JOIN FETCH j.employer e
            WHERE j.status = :status
            ORDER BY j.postedAt DESC
            """)
    Page<Job> findDetailedByStatus(@Param("status") JobStatus status, Pageable pageable);

    @Query("""
            SELECT j FROM Job j
            LEFT JOIN FETCH j.employer e
            WHERE e.id = :employerId
            ORDER BY j.postedAt DESC
            """)
    Page<Job> findDetailedByEmployerId(@Param("employerId") Long employerId, Pageable pageable);

        List<Job> findByEmployer_Id(Long employerId);

        void deleteByEmployer_Id(Long employerId);

    long countByStatus(JobStatus status);
}
