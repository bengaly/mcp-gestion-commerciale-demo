package com.enterprise.mcp.config;

import com.enterprise.mcp.domain.entity.*;
import com.enterprise.mcp.domain.repository.CustomerRepository;
import com.enterprise.mcp.domain.repository.InvoiceRepository;
import com.enterprise.mcp.domain.repository.OrderRepository;
import com.enterprise.mcp.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Initialisation des données de démonstration
 * 
 * Ce composant crée des données réalistes pour tester les capacités MCP.
 * En production, ces données viendraient de la vraie base de données.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final InvoiceRepository invoiceRepository;
    private final ProductRepository productRepository;
    
    @Override
    @Transactional
    public void run(String... args) {
        log.info("Initialisation des données de démonstration...");
        
        // Création des produits
        createProducts();
        
        // Création des clients
        Customer techCorp = createCustomer("CLI-001", "TechCorp Solutions", "Jean Dupont", 
            "jean.dupont@techcorp.fr", Customer.CustomerSegment.ENTERPRISE, 100000.0);
        
        Customer pmeInnovation = createCustomer("CLI-002", "PME Innovation", "Marie Martin", 
            "marie.martin@pme-innovation.fr", Customer.CustomerSegment.PREMIUM, 50000.0);
        
        Customer startupDigital = createCustomer("CLI-003", "Startup Digital", "Pierre Durand", 
            "pierre.durand@startup-digital.fr", Customer.CustomerSegment.STANDARD, 20000.0);
        
        Customer grandGroupe = createCustomer("CLI-004", "Grand Groupe SA", "Sophie Bernard", 
            "sophie.bernard@grandgroupe.fr", Customer.CustomerSegment.VIP, 500000.0);
        
        // Création des commandes pour TechCorp
        Order order1 = createOrder(techCorp, "CMD-20240115-TC001", Order.OrderStatus.DELIVERED);
        addOrderLine(order1, "PROD-001", "Licence Logiciel Enterprise", 5, new BigDecimal("2500.00"));
        addOrderLine(order1, "PROD-002", "Support Premium 1 an", 1, new BigDecimal("5000.00"));
        order1.calculateTotals();
        orderRepository.save(order1);
        
        Order order2 = createOrder(techCorp, "CMD-20240210-TC002", Order.OrderStatus.IN_PREPARATION);
        addOrderLine(order2, "PROD-003", "Formation Expert", 3, new BigDecimal("1500.00"));
        order2.calculateTotals();
        orderRepository.save(order2);
        
        // Création des commandes pour PME Innovation
        Order order3 = createOrder(pmeInnovation, "CMD-20240118-PME001", Order.OrderStatus.DELIVERED);
        addOrderLine(order3, "PROD-001", "Licence Logiciel Enterprise", 2, new BigDecimal("2500.00"));
        order3.calculateTotals();
        orderRepository.save(order3);
        
        Order order4 = createOrder(pmeInnovation, "CMD-20240305-PME002", Order.OrderStatus.PENDING_VALIDATION);
        addOrderLine(order4, "PROD-004", "Module Analytics", 1, new BigDecimal("3500.00"));
        addOrderLine(order4, "PROD-005", "Intégration API", 1, new BigDecimal("2000.00"));
        order4.calculateTotals();
        orderRepository.save(order4);
        
        // Création des commandes pour Grand Groupe
        Order order5 = createOrder(grandGroupe, "CMD-20240105-GG001", Order.OrderStatus.DELIVERED);
        addOrderLine(order5, "PROD-001", "Licence Logiciel Enterprise", 50, new BigDecimal("2000.00"));
        addOrderLine(order5, "PROD-002", "Support Premium 1 an", 50, new BigDecimal("1000.00"));
        addOrderLine(order5, "PROD-006", "Déploiement sur site", 1, new BigDecimal("15000.00"));
        order5.calculateTotals();
        orderRepository.save(order5);
        
        // Création des factures
        Invoice inv1 = createInvoice(techCorp, order1, "FAC-2024-000123", Invoice.InvoiceStatus.PAID);
        inv1.setPaidAmount(inv1.getTotalAmount());
        inv1.setRemainingAmount(BigDecimal.ZERO);
        inv1.setPaidDate(LocalDate.now().minusDays(10));
        invoiceRepository.save(inv1);
        
        Invoice inv2 = createInvoice(pmeInnovation, order3, "FAC-2024-000124", Invoice.InvoiceStatus.SENT);
        inv2.setDueDate(LocalDate.now().plusDays(15));
        invoiceRepository.save(inv2);
        
        Invoice inv3 = createInvoice(grandGroupe, order5, "FAC-2024-000125", Invoice.InvoiceStatus.PARTIALLY_PAID);
        inv3.setPaidAmount(new BigDecimal("100000.00"));
        inv3.setRemainingAmount(inv3.getTotalAmount().subtract(inv3.getPaidAmount()));
        inv3.setDueDate(LocalDate.now().plusDays(30));
        invoiceRepository.save(inv3);
        
        // Facture en retard pour Startup Digital
        Invoice inv4 = createInvoice(startupDigital, null, "FAC-2024-000100", Invoice.InvoiceStatus.OVERDUE);
        addInvoiceLine(inv4, "Services de conseil", 10, new BigDecimal("150.00"));
        inv4.calculateTotals();
        inv4.setDueDate(LocalDate.now().minusDays(45));
        inv4.setIssueDate(LocalDate.now().minusDays(75));
        invoiceRepository.save(inv4);
        
        log.info("Données de démonstration initialisées avec succès!");
        log.info("- {} produits créés", productRepository.count());
        log.info("- {} clients créés", customerRepository.count());
        log.info("- {} commandes créées", orderRepository.count());
        log.info("- {} factures créées", invoiceRepository.count());
    }
    
    private void createProducts() {
        // Logiciels
        createProduct("PROD-001", "Licence Logiciel Enterprise", "Licence annuelle du logiciel Enterprise Edition",
            Product.ProductCategory.SOFTWARE, new BigDecimal("2500.00"), 100, "licence");
        createProduct("PROD-007", "Licence Logiciel Standard", "Licence annuelle du logiciel Standard Edition",
            Product.ProductCategory.SOFTWARE, new BigDecimal("1200.00"), 200, "licence");
        
        // Services
        createProduct("PROD-002", "Support Premium 1 an", "Contrat de support premium avec SLA garanti",
            Product.ProductCategory.SERVICE, new BigDecimal("5000.00"), null, "contrat");
        createProduct("PROD-003", "Formation Expert", "Formation avancée de 2 jours sur site",
            Product.ProductCategory.SERVICE, new BigDecimal("1500.00"), null, "session");
        createProduct("PROD-006", "Déploiement sur site", "Installation et configuration sur site client",
            Product.ProductCategory.SERVICE, new BigDecimal("15000.00"), null, "prestation");
        
        // Modules et extensions
        createProduct("PROD-004", "Module Analytics", "Module d'analyse avancée et reporting",
            Product.ProductCategory.SUBSCRIPTION, new BigDecimal("3500.00"), 50, "module");
        createProduct("PROD-005", "Intégration API", "Pack d'intégration API REST complète",
            Product.ProductCategory.SUBSCRIPTION, new BigDecimal("2000.00"), 50, "pack");
        
        // Hardware
        createProduct("P-LAPTOP-001", "Laptop Pro 15", "Ordinateur portable professionnel 15 pouces",
            Product.ProductCategory.HARDWARE, new BigDecimal("1299.00"), 25, "unité");
        createProduct("P-LAPTOP-002", "Laptop Ultra 14", "Ordinateur portable ultraléger 14 pouces",
            Product.ProductCategory.HARDWARE, new BigDecimal("1599.00"), 15, "unité");
        createProduct("P-DESKTOP-001", "Workstation Pro", "Station de travail haute performance",
            Product.ProductCategory.HARDWARE, new BigDecimal("2499.00"), 10, "unité");
        
        // Accessoires
        createProduct("P-MOUSE-001", "Souris Ergonomique", "Souris sans fil ergonomique",
            Product.ProductCategory.ACCESSORY, new BigDecimal("79.00"), 100, "unité");
        createProduct("P-KEYBOARD-001", "Clavier Mécanique", "Clavier mécanique rétroéclairé",
            Product.ProductCategory.ACCESSORY, new BigDecimal("149.00"), 75, "unité");
        createProduct("P-MONITOR-001", "Écran 27 4K", "Moniteur 27 pouces 4K UHD",
            Product.ProductCategory.ACCESSORY, new BigDecimal("549.00"), 30, "unité");
        createProduct("P-DOCK-001", "Station d'accueil USB-C", "Station d'accueil universelle USB-C",
            Product.ProductCategory.ACCESSORY, new BigDecimal("199.00"), 40, "unité");
    }
    
    private void createProduct(String code, String name, String description,
                               Product.ProductCategory category, BigDecimal price, 
                               Integer stock, String unit) {
        Product product = Product.builder()
            .productCode(code)
            .name(name)
            .description(description)
            .category(category)
            .unitPrice(price)
            .stockQuantity(stock)
            .status(Product.ProductStatus.ACTIVE)
            .unit(unit)
            .build();
        productRepository.save(product);
    }
    
    private Customer createCustomer(String code, String company, String contact, String email, 
                                    Customer.CustomerSegment segment, Double creditLimit) {
        Customer customer = Customer.builder()
            .customerCode(code)
            .companyName(company)
            .contactName(contact)
            .email(email)
            .phone("+33 1 23 45 67 89")
            .address("123 Rue de l'Entreprise, 75001 Paris")
            .city("Paris")
            .country("France")
            .status(Customer.CustomerStatus.ACTIVE)
            .segment(segment)
            .creditLimit(creditLimit)
            .build();
        return customerRepository.save(customer);
    }
    
    private Order createOrder(Customer customer, String orderNumber, Order.OrderStatus status) {
        return Order.builder()
            .orderNumber(orderNumber)
            .customer(customer)
            .status(status)
            .shippingAddress(customer.getAddress())
            .billingAddress(customer.getAddress())
            .orderDate(LocalDateTime.now().minusDays((long)(Math.random() * 60)))
            .expectedDeliveryDate(LocalDateTime.now().plusDays(7))
            .createdBy("system")
            .build();
    }
    
    private void addOrderLine(Order order, String productCode, String productName, 
                              int quantity, BigDecimal unitPrice) {
        OrderLine line = OrderLine.builder()
            .productCode(productCode)
            .productName(productName)
            .quantity(quantity)
            .unitPrice(unitPrice)
            .discountPercent(BigDecimal.ZERO)
            .build();
        order.addLine(line);
    }
    
    private Invoice createInvoice(Customer customer, Order order, String invoiceNumber, 
                                  Invoice.InvoiceStatus status) {
        Invoice invoice = Invoice.builder()
            .invoiceNumber(invoiceNumber)
            .customer(customer)
            .order(order)
            .status(status)
            .issueDate(LocalDate.now().minusDays(30))
            .dueDate(LocalDate.now().plusDays(30))
            .paymentTerms("Net 30 jours")
            .billingAddress(customer.getAddress())
            .paidAmount(BigDecimal.ZERO)
            .build();
        
        if (order != null) {
            // Copier les lignes de commande vers la facture
            for (OrderLine orderLine : order.getLines()) {
                InvoiceLine invoiceLine = InvoiceLine.builder()
                    .description(orderLine.getProductName() + " (" + orderLine.getProductCode() + ")")
                    .quantity(orderLine.getQuantity())
                    .unitPrice(orderLine.getUnitPrice())
                    .discountPercent(orderLine.getDiscountPercent())
                    .build();
                invoice.addLine(invoiceLine);
            }
            invoice.calculateTotals();
        }
        
        return invoice;
    }
    
    private void addInvoiceLine(Invoice invoice, String description, int quantity, BigDecimal unitPrice) {
        InvoiceLine line = InvoiceLine.builder()
            .description(description)
            .quantity(quantity)
            .unitPrice(unitPrice)
            .discountPercent(BigDecimal.ZERO)
            .build();
        invoice.addLine(line);
    }
}
