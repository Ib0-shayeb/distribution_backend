package com.example.distribution_backernd.repository;

import com.example.distribution_backernd.model.Trip;
import com.example.distribution_backernd.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, Integer> {
    Optional<Trip> findById(Integer tripId);
}
