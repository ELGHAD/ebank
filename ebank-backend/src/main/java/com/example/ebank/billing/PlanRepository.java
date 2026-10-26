package com.example.ebank.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanRepository extends JpaRepository<Plan, String> {
    List<Plan> findAllByOrderBySortOrderAsc();
}
