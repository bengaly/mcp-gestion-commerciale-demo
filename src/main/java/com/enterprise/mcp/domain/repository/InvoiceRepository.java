package com.enterprise.mcp.domain.repository;

import com.enterprise.mcp.domain.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour les opérations sur les factures
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    
    List<Invoice> findByCustomerId(Long customerId);
    
    List<Invoice> findByCustomerCustomerCode(String customerCode);
    
    List<Invoice> findByStatus(Invoice.InvoiceStatus status);
    
    Optional<Invoice> findByOrderId(Long orderId);
    
    @Query("SELECT i FROM Invoice i WHERE i.customer.id = :customerId ORDER BY i.issueDate DESC")
    List<Invoice> findRecentInvoicesByCustomer(@Param("customerId") Long customerId);
    
    @Query("SELECT i FROM Invoice i WHERE i.status != 'PAID' AND i.dueDate < :today")
    List<Invoice> findOverdueInvoices(@Param("today") LocalDate today);
    
    @Query("SELECT i FROM Invoice i WHERE i.customer.id = :customerId AND i.status != 'PAID' AND i.status != 'CANCELLED'")
    List<Invoice> findUnpaidInvoicesByCustomer(@Param("customerId") Long customerId);
    
    @Query("SELECT SUM(i.totalAmount) FROM Invoice i WHERE i.customer.id = :customerId AND i.status = 'PAID'")
    Double getTotalPaidByCustomer(@Param("customerId") Long customerId);
    
    @Query("SELECT SUM(i.remainingAmount) FROM Invoice i WHERE i.customer.id = :customerId AND i.status != 'PAID' AND i.status != 'CANCELLED'")
    Double getTotalOutstandingByCustomer(@Param("customerId") Long customerId);
    
    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.customer.id = :customerId")
    Long countInvoicesByCustomer(@Param("customerId") Long customerId);
    
    boolean existsByInvoiceNumber(String invoiceNumber);
}
