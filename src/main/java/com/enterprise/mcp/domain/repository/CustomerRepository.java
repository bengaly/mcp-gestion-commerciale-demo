package com.enterprise.mcp.domain.repository;

import com.enterprise.mcp.domain.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour les opérations sur les clients
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    
    Optional<Customer> findByCustomerCode(String customerCode);
    
    List<Customer> findByStatus(Customer.CustomerStatus status);
    
    List<Customer> findBySegment(Customer.CustomerSegment segment);
    
    @Query("SELECT c FROM Customer c WHERE LOWER(c.companyName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Customer> searchByCompanyName(@Param("name") String name);
    
    @Query("SELECT c FROM Customer c WHERE c.status = 'ACTIVE' ORDER BY c.companyName")
    List<Customer> findAllActiveCustomers();
    
    boolean existsByCustomerCode(String customerCode);
}
