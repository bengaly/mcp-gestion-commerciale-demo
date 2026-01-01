package com.enterprise.mcp.service;

import com.enterprise.mcp.domain.entity.Customer;
import com.enterprise.mcp.domain.entity.Invoice;
import com.enterprise.mcp.domain.entity.Order;
import com.enterprise.mcp.domain.repository.CustomerRepository;
import com.enterprise.mcp.domain.repository.InvoiceRepository;
import com.enterprise.mcp.domain.repository.OrderRepository;
import com.enterprise.mcp.service.dto.CustomerActivitySummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service métier pour la gestion des clients
 * 
 * Ce service encapsule toute la logique métier liée aux clients.
 * Il est utilisé par les capacités MCP mais aussi par le reste de l'application.
 * L'IA n'accède JAMAIS directement à ce service - elle passe par les capacités MCP.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CustomerService {
    
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final InvoiceRepository invoiceRepository;
    
    /**
     * Recherche un client par son code unique
     */
    public Optional<Customer> findByCode(String customerCode) {
        log.debug("Recherche client par code: {}", customerCode);
        return customerRepository.findByCustomerCode(customerCode);
    }
    
    /**
     * Recherche un client par son ID
     */
    public Optional<Customer> findById(Long id) {
        log.debug("Recherche client par ID: {}", id);
        return customerRepository.findById(id);
    }
    
    /**
     * Recherche des clients par nom d'entreprise
     */
    public List<Customer> searchByCompanyName(String name) {
        log.debug("Recherche clients par nom: {}", name);
        return customerRepository.searchByCompanyName(name);
    }
    
    /**
     * Liste tous les clients actifs
     */
    public List<Customer> findAllActiveCustomers() {
        return customerRepository.findAllActiveCustomers();
    }
    
    /**
     * Génère un résumé complet de l'activité d'un client
     * 
     * Cette méthode agrège les données de commandes et factures
     * pour fournir une vue synthétique exploitable par l'IA.
     */
    public Optional<CustomerActivitySummary> summarizeActivity(String customerCode) {
        log.info("Génération du résumé d'activité pour le client: {}", customerCode);
        
        return customerRepository.findByCustomerCode(customerCode)
            .map(customer -> {
                Long customerId = customer.getId();
                
                // Récupération des commandes
                List<Order> recentOrders = orderRepository.findRecentOrdersByCustomer(customerId);
                Long totalOrders = orderRepository.countOrdersByCustomer(customerId);
                Double totalRevenue = orderRepository.getTotalRevenueByCustomer(customerId);
                
                // Récupération des factures
                List<Invoice> recentInvoices = invoiceRepository.findRecentInvoicesByCustomer(customerId);
                List<Invoice> unpaidInvoices = invoiceRepository.findUnpaidInvoicesByCustomer(customerId);
                Long totalInvoices = invoiceRepository.countInvoicesByCustomer(customerId);
                Double totalPaid = invoiceRepository.getTotalPaidByCustomer(customerId);
                Double totalOutstanding = invoiceRepository.getTotalOutstandingByCustomer(customerId);
                
                // Construction du résumé
                return CustomerActivitySummary.builder()
                    .customer(customer)
                    .totalOrders(totalOrders != null ? totalOrders.intValue() : 0)
                    .totalRevenue(totalRevenue != null ? BigDecimal.valueOf(totalRevenue) : BigDecimal.ZERO)
                    .recentOrders(recentOrders.stream().limit(5).toList())
                    .totalInvoices(totalInvoices != null ? totalInvoices.intValue() : 0)
                    .totalPaid(totalPaid != null ? BigDecimal.valueOf(totalPaid) : BigDecimal.ZERO)
                    .totalOutstanding(totalOutstanding != null ? BigDecimal.valueOf(totalOutstanding) : BigDecimal.ZERO)
                    .unpaidInvoicesCount(unpaidInvoices.size())
                    .recentInvoices(recentInvoices.stream().limit(5).toList())
                    .hasOverdueInvoices(unpaidInvoices.stream().anyMatch(Invoice::isOverdue))
                    .generatedAt(LocalDateTime.now())
                    .build();
            });
    }
    
    /**
     * Vérifie si un client peut passer une nouvelle commande
     * Règles métier : client actif, pas trop de factures impayées, crédit suffisant
     */
    public boolean canPlaceOrder(String customerCode, BigDecimal orderAmount) {
        return customerRepository.findByCustomerCode(customerCode)
            .map(customer -> {
                // Vérification du statut
                if (customer.getStatus() != Customer.CustomerStatus.ACTIVE) {
                    log.warn("Client {} non actif - statut: {}", customerCode, customer.getStatus());
                    return false;
                }
                
                // Vérification des impayés
                Double outstanding = invoiceRepository.getTotalOutstandingByCustomer(customer.getId());
                if (outstanding != null && customer.getCreditLimit() != null) {
                    BigDecimal totalExposure = BigDecimal.valueOf(outstanding).add(orderAmount);
                    if (totalExposure.compareTo(BigDecimal.valueOf(customer.getCreditLimit())) > 0) {
                        log.warn("Client {} - dépassement de limite de crédit", customerCode);
                        return false;
                    }
                }
                
                return true;
            })
            .orElse(false);
    }
    
    /**
     * Création d'un nouveau client
     */
    @Transactional
    public Customer createCustomer(Customer customer) {
        log.info("Création d'un nouveau client: {}", customer.getCompanyName());
        
        if (customerRepository.existsByCustomerCode(customer.getCustomerCode())) {
            throw new IllegalArgumentException("Code client déjà existant: " + customer.getCustomerCode());
        }
        
        return customerRepository.save(customer);
    }
    
    /**
     * Mise à jour d'un client existant
     */
    @Transactional
    public Customer updateCustomer(Customer customer) {
        log.info("Mise à jour du client: {}", customer.getCustomerCode());
        
        if (!customerRepository.existsById(customer.getId())) {
            throw new IllegalArgumentException("Client non trouvé: " + customer.getId());
        }
        
        return customerRepository.save(customer);
    }
}
