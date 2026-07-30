package com.example.distribution_backernd.repository;

import com.example.distribution_backernd.model.Authority;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuthorityRepository extends JpaRepository<Authority, Integer> {
    List<Authority> findByUsername(String username);
}