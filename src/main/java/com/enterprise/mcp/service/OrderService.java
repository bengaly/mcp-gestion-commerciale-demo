package com.enterprise.mcp.service;

import com.enterprise.mcp.domain.entity.Customer;
import com.enterprise.mcp.domain.entity.Order;
import com.enterprise.mcp.domain.entity.OrderLine;
import com.enterprise.mcp.domain.repository.CustomerRepository;
import com.enterprise.mcp.domain.repository.OrderRepository;
import com.enterprise.mcp.service.dto.CreateOrderRequest;
import com.enterprise.mcp.service.dto.OrderValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service métier pour la gestion des commandes
 * 
 * Ce service encapsule toute la logique métier liée aux commandes.
 * Points clés :
 * - Validation métier systématique
 * - Génération automatique des numéros de commande
 * - Calcul des totaux
 * - Gestion des statuts
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    
    /**
     * Recherche une commande par son numéro
     */
    public Optional<Order> findByOrderNumber(String orderNumber) {
        log.debug("Recherche commande par numéro: {}", orderNumber);
        return orderRepository.findByOrderNumber(orderNumber);
    }
    
    /**
     * Recherche une commande par son ID
     */
    public Optional<Order> findById(Long id) {
        log.debug("Recherche commande par ID: {}", id);
        return orderRepository.findById(id);
    }
    
    /**
     * Liste les commandes d'un client
     */
    public List<Order> findByCustomerCode(String customerCode) {
        log.debug("Recherche commandes pour client: {}", customerCode);
        return orderRepository.findByCustomerCustomerCode(customerCode);
    }
    
    /**
     * Liste les commandes par statut
     */
    public List<Order> findByStatus(Order.OrderStatus status) {
        return orderRepository.findByStatus(status);
    }
    
    /**
     * Liste les commandes récentes d'un client
     */
    public List<Order> findRecentOrdersByCustomer(Long customerId) {
        return orderRepository.findRecentOrdersByCustomer(customerId);
    }
    
    /**
     * Valide une demande de création de commande AVANT de la créer
     * 
     * Cette méthode est cruciale pour MCP : elle permet à l'IA de vérifier
     * si une commande est valide avant de demander confirmation à l'utilisateur.
     */
    public OrderValidationResult validateOrderRequest(CreateOrderRequest request) {
        log.info("Validation de la demande de commande pour client: {}", request.getCustomerCode());
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Vérification du client
        Optional<Customer> customerOpt = customerRepository.findByCustomerCode(request.getCustomerCode());
        if (customerOpt.isEmpty()) {
            errors.add("Client non trouvé: " + request.getCustomerCode());
            return OrderValidationResult.invalid(errors);
        }
        
        Customer customer = customerOpt.get();
        
        // Vérification du statut client
        if (customer.getStatus() != Customer.CustomerStatus.ACTIVE) {
            errors.add("Le client n'est pas actif. Statut actuel: " + customer.getStatus());
        }
        
        // Vérification des lignes de commande
        if (request.getLines() == null || request.getLines().isEmpty()) {
            errors.add("La commande doit contenir au moins une ligne");
        } else {
            // Calcul du montant total estimé
            BigDecimal estimatedTotal = request.getLines().stream()
                .map(line -> line.getUnitPrice().multiply(new BigDecimal(line.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Vérification de la limite de crédit
            if (!customerService.canPlaceOrder(request.getCustomerCode(), estimatedTotal)) {
                errors.add("Dépassement de la limite de crédit du client");
            }
            
            // Vérification des quantités
            for (var line : request.getLines()) {
                if (line.getQuantity() <= 0) {
                    errors.add("Quantité invalide pour le produit: " + line.getProductCode());
                }
                if (line.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add("Prix unitaire invalide pour le produit: " + line.getProductCode());
                }
            }
            
            // Avertissement pour commandes importantes
            if (estimatedTotal.compareTo(new BigDecimal("10000")) > 0) {
                warnings.add("Commande supérieure à 10 000€ - validation managériale recommandée");
            }
        }
        
        // Vérification de l'adresse de livraison
        if (request.getShippingAddress() == null || request.getShippingAddress().isBlank()) {
            warnings.add("Aucune adresse de livraison spécifiée - l'adresse du client sera utilisée");
        }
        
        if (errors.isEmpty()) {
            return OrderValidationResult.valid(warnings, calculateEstimatedTotal(request));
        } else {
            return OrderValidationResult.invalid(errors);
        }
    }
    
    /**
     * Crée une nouvelle commande après validation
     * 
     * IMPORTANT : Cette méthode ne doit être appelée qu'après validation
     * et confirmation explicite de l'utilisateur via MCP.
     */
    @Transactional
    public Order createOrder(CreateOrderRequest request, String createdBy) {
        log.info("Création de commande pour client: {} par: {}", request.getCustomerCode(), createdBy);
        
        // Re-validation par sécurité
        OrderValidationResult validation = validateOrderRequest(request);
        if (!validation.isValid()) {
            throw new IllegalArgumentException("Commande invalide: " + String.join(", ", validation.getErrors()));
        }
        
        Customer customer = customerRepository.findByCustomerCode(request.getCustomerCode())
            .orElseThrow(() -> new IllegalArgumentException("Client non trouvé"));
        
        // Génération du numéro de commande
        String orderNumber = generateOrderNumber();
        
        // Création de la commande
        Order order = Order.builder()
            .orderNumber(orderNumber)
            .customer(customer)
            .status(Order.OrderStatus.PENDING_VALIDATION)
            .shippingAddress(request.getShippingAddress() != null ? 
                request.getShippingAddress() : customer.getAddress())
            .billingAddress(customer.getAddress())
            .notes(request.getNotes())
            .expectedDeliveryDate(request.getExpectedDeliveryDate())
            .createdBy(createdBy)
            .build();
        
        // Ajout des lignes
        for (var lineRequest : request.getLines()) {
            OrderLine line = OrderLine.builder()
                .productCode(lineRequest.getProductCode())
                .productName(lineRequest.getProductName())
                .quantity(lineRequest.getQuantity())
                .unitPrice(lineRequest.getUnitPrice())
                .discountPercent(lineRequest.getDiscountPercent())
                .notes(lineRequest.getNotes())
                .build();
            order.addLine(line);
        }
        
        // Calcul des totaux
        order.calculateTotals();
        
        Order savedOrder = orderRepository.save(order);
        log.info("Commande créée avec succès: {}", savedOrder.getOrderNumber());
        
        return savedOrder;
    }
    
    /**
     * Met à jour le statut d'une commande
     */
    @Transactional
    public Order updateStatus(String orderNumber, Order.OrderStatus newStatus, String updatedBy) {
        log.info("Mise à jour statut commande {} vers {} par {}", orderNumber, newStatus, updatedBy);
        
        Order order = orderRepository.findByOrderNumber(orderNumber)
            .orElseThrow(() -> new IllegalArgumentException("Commande non trouvée: " + orderNumber));
        
        // Validation des transitions de statut
        validateStatusTransition(order.getStatus(), newStatus);
        
        order.setStatus(newStatus);
        
        if (newStatus == Order.OrderStatus.DELIVERED) {
            order.setActualDeliveryDate(LocalDateTime.now());
        }
        
        return orderRepository.save(order);
    }
    
    /**
     * Annule une commande
     */
    @Transactional
    public Order cancelOrder(String orderNumber, String reason, String cancelledBy) {
        log.info("Annulation commande {} - Raison: {} - Par: {}", orderNumber, reason, cancelledBy);
        
        Order order = orderRepository.findByOrderNumber(orderNumber)
            .orElseThrow(() -> new IllegalArgumentException("Commande non trouvée: " + orderNumber));
        
        if (order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new IllegalStateException("Impossible d'annuler une commande déjà livrée");
        }
        
        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new IllegalStateException("La commande est déjà annulée");
        }
        
        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setNotes((order.getNotes() != null ? order.getNotes() + "\n" : "") + 
            "Annulée le " + LocalDateTime.now() + " par " + cancelledBy + ": " + reason);
        
        return orderRepository.save(order);
    }
    
    private String generateOrderNumber() {
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "CMD-" + datePrefix + "-" + uniqueSuffix;
    }
    
    private BigDecimal calculateEstimatedTotal(CreateOrderRequest request) {
        return request.getLines().stream()
            .map(line -> {
                BigDecimal subtotal = line.getUnitPrice().multiply(new BigDecimal(line.getQuantity()));
                if (line.getDiscountPercent() != null) {
                    BigDecimal discount = subtotal.multiply(line.getDiscountPercent()).divide(new BigDecimal("100"));
                    subtotal = subtotal.subtract(discount);
                }
                return subtotal;
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private void validateStatusTransition(Order.OrderStatus current, Order.OrderStatus target) {
        // Règles de transition de statut
        boolean valid = switch (current) {
            case DRAFT -> target == Order.OrderStatus.PENDING_VALIDATION || target == Order.OrderStatus.CANCELLED;
            case PENDING_VALIDATION -> target == Order.OrderStatus.VALIDATED || target == Order.OrderStatus.CANCELLED;
            case VALIDATED -> target == Order.OrderStatus.IN_PREPARATION || target == Order.OrderStatus.CANCELLED;
            case IN_PREPARATION -> target == Order.OrderStatus.SHIPPED || target == Order.OrderStatus.CANCELLED;
            case SHIPPED -> target == Order.OrderStatus.DELIVERED || target == Order.OrderStatus.RETURNED;
            case DELIVERED -> target == Order.OrderStatus.RETURNED;
            case CANCELLED, RETURNED -> false;
        };
        
        if (!valid) {
            throw new IllegalStateException(
                String.format("Transition de statut invalide: %s -> %s", current, target)
            );
        }
    }
}
