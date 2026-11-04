package com.visa.repository;

import com.visa.entity.VisaApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.*;

public interface ApplicationRepository extends JpaRepository<VisaApplication, Long> {
    @EntityGraph(attributePaths = "user")
    List<VisaApplication> findByUserIdOrderByUpdatedAtDesc(Long userId);

    @Override
    @EntityGraph(attributePaths = "user")
    Optional<VisaApplication> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "user")
    List<VisaApplication> findAll();
}
