package com.enterprise.mcp.service;

import com.enterprise.mcp.domain.entity.Invoice;
import com.enterprise.mcp.domain.entity.Order;
import com.enterprise.mcp.domain.repository.InvoiceRepository;
import com.enterprise.mcp.service.dto.InvoiceAnalysis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service métier pour la gestion des factures
 * 
 * Ce service encapsule toute la logique métier liée aux factures.
 * Il fournit notamment des capacités d'analyse avancées pour l'IA.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class InvoiceService {
    
    private final InvoiceRepository invoiceRepository;
    
    /**
     * Recherche une facture par son numéro
     */
    public Optional<Invoice> findByInvoiceNumber(String invoiceNumber) {
        log.debug("Recherche facture par numéro: {}", invoiceNumber);
        return invoiceRepository.findByInvoiceNumber(invoiceNumber);
    }
    
    /**
     * Recherche une facture par son ID
     */
    public Optional<Invoice> findById(Long id) {
        log.debug("Recherche facture par ID: {}", id);
        return invoiceRepository.findById(id);
    }
    
    /**
     * Liste les factures d'un client
     */
    public List<Invoice> findByCustomerCode(String customerCode) {
        log.debug("Recherche factures pour client: {}", customerCode);
        return invoiceRepository.findByCustomerCustomerCode(customerCode);
    }
    
    /**
     * Liste les factures impayées d'un client
     */
    public List<Invoice> findUnpaidByCustomer(Long customerId) {
        return invoiceRepository.findUnpaidInvoicesByCustomer(customerId);
    }
    
    /**
     * Liste toutes les factures en retard
     */
    public List<Invoice> findOverdueInvoices() {
        return invoiceRepository.findOverdueInvoices(LocalDate.now());
    }
    
    /**
     * Analyse détaillée d'une facture
     * 
     * Cette méthode génère une analyse complète d'une facture,
     * incluant des indicateurs de risque et des recommandations.
     * C'est la capacité principale exposée via MCP pour l'analyse de factures.
     */
    public Optional<InvoiceAnalysis> analyzeInvoice(String invoiceNumber) {
        log.info("Analyse de la facture: {}", invoiceNumber);
        
        return invoiceRepository.findByInvoiceNumber(invoiceNumber)
            .map(invoice -> {
                InvoiceAnalysis.InvoiceAnalysisBuilder builder = InvoiceAnalysis.builder()
                    .invoice(invoice)
                    .invoiceNumber(invoice.getInvoiceNumber())
                    .customerName(invoice.getCustomer().getCompanyName())
                    .customerCode(invoice.getCustomer().getCustomerCode())
                    .status(invoice.getStatus().name())
                    .totalAmount(invoice.getTotalAmount())
                    .paidAmount(invoice.getPaidAmount())
                    .remainingAmount(invoice.getRemainingAmount())
                    .issueDate(invoice.getIssueDate())
                    .dueDate(invoice.getDueDate())
                    .isOverdue(invoice.isOverdue())
                    .daysOverdue(invoice.getDaysOverdue());
                
                // Calcul du pourcentage payé
                if (invoice.getTotalAmount() != null && invoice.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal paidPercent = invoice.getPaidAmount()
                        .divide(invoice.getTotalAmount(), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                    builder.paidPercentage(paidPercent);
                } else {
                    builder.paidPercentage(BigDecimal.ZERO);
                }
                
                // Évaluation du risque
                String riskLevel = evaluateRiskLevel(invoice);
                builder.riskLevel(riskLevel);
                
                // Recommandations
                List<String> recommendations = generateRecommendations(invoice);
                builder.recommendations(recommendations);
                
                // Historique du client
                Long customerId = invoice.getCustomer().getId();
                Double totalPaid = invoiceRepository.getTotalPaidByCustomer(customerId);
                Double totalOutstanding = invoiceRepository.getTotalOutstandingByCustomer(customerId);
                Long invoiceCount = invoiceRepository.countInvoicesByCustomer(customerId);
                
                builder.customerTotalPaid(totalPaid != null ? BigDecimal.valueOf(totalPaid) : BigDecimal.ZERO);
                builder.customerTotalOutstanding(totalOutstanding != null ? BigDecimal.valueOf(totalOutstanding) : BigDecimal.ZERO);
                builder.customerInvoiceCount(invoiceCount != null ? invoiceCount.intValue() : 0);
                
                return builder.build();
            });
    }
    
    /**
     * Évalue le niveau de risque d'une facture
     */
    private String evaluateRiskLevel(Invoice invoice) {
        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
            return "AUCUN";
        }
        
        if (invoice.isOverdue()) {
            long daysOverdue = invoice.getDaysOverdue();
            if (daysOverdue > 90) {
                return "CRITIQUE";
            } else if (daysOverdue > 60) {
                return "ÉLEVÉ";
            } else if (daysOverdue > 30) {
                return "MOYEN";
            } else {
                return "FAIBLE";
            }
        }
        
        // Facture non échue
        if (invoice.getDueDate() != null) {
            long daysUntilDue = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), invoice.getDueDate());
            if (daysUntilDue <= 7) {
                return "ATTENTION";
            }
        }
        
        return "NORMAL";
    }
    
    /**
     * Génère des recommandations basées sur l'état de la facture
     */
    private List<String> generateRecommendations(Invoice invoice) {
        java.util.ArrayList<String> recommendations = new java.util.ArrayList<>();
        
        switch (invoice.getStatus()) {
            case DRAFT:
                recommendations.add("Finaliser et émettre la facture");
                break;
            case ISSUED:
                recommendations.add("Envoyer la facture au client");
                break;
            case SENT:
                if (invoice.getDueDate() != null) {
                    long daysUntilDue = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), invoice.getDueDate());
                    if (daysUntilDue <= 7 && daysUntilDue > 0) {
                        recommendations.add("Envoyer un rappel de paiement - échéance proche");
                    }
                }
                break;
            case OVERDUE:
                long daysOverdue = invoice.getDaysOverdue();
                if (daysOverdue <= 15) {
                    recommendations.add("Envoyer une première relance");
                } else if (daysOverdue <= 30) {
                    recommendations.add("Envoyer une deuxième relance - contacter le client par téléphone");
                } else if (daysOverdue <= 60) {
                    recommendations.add("Escalader au service recouvrement");
                    recommendations.add("Envisager la suspension du compte client");
                } else {
                    recommendations.add("Transférer au contentieux");
                    recommendations.add("Provisionner la créance douteuse");
                }
                break;
            case PARTIALLY_PAID:
                recommendations.add("Relancer pour le solde restant: " + 
                    String.format("%,.2f €", invoice.getRemainingAmount()));
                break;
            case DISPUTED:
                recommendations.add("Analyser le litige avec le service commercial");
                recommendations.add("Contacter le client pour résolution");
                break;
            default:
                break;
        }
        
        // Recommandations générales basées sur le client
        if (invoice.getCustomer().getSegment() != null) {
            switch (invoice.getCustomer().getSegment()) {
                case VIP, ENTERPRISE:
                    recommendations.add("Client prioritaire - traitement personnalisé recommandé");
                    break;
                default:
                    break;
            }
        }
        
        return recommendations;
    }
    
    /**
     * Enregistre un paiement sur une facture
     */
    @Transactional
    public Invoice recordPayment(String invoiceNumber, BigDecimal amount, String paymentReference) {
        log.info("Enregistrement paiement de {} sur facture {}", amount, invoiceNumber);
        
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
            .orElseThrow(() -> new IllegalArgumentException("Facture non trouvée: " + invoiceNumber));
        
        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
            throw new IllegalStateException("La facture est déjà entièrement payée");
        }
        
        if (invoice.getStatus() == Invoice.InvoiceStatus.CANCELLED) {
            throw new IllegalStateException("Impossible d'enregistrer un paiement sur une facture annulée");
        }
        
        BigDecimal newPaidAmount = invoice.getPaidAmount().add(amount);
        invoice.setPaidAmount(newPaidAmount);
        invoice.setRemainingAmount(invoice.getTotalAmount().subtract(newPaidAmount));
        
        // Mise à jour du statut
        if (invoice.getRemainingAmount().compareTo(BigDecimal.ZERO) <= 0) {
            invoice.setStatus(Invoice.InvoiceStatus.PAID);
            invoice.setPaidDate(LocalDate.now());
        } else {
            invoice.setStatus(Invoice.InvoiceStatus.PARTIALLY_PAID);
        }
        
        // Ajout d'une note
        String note = String.format("Paiement de %,.2f € reçu le %s (Réf: %s)", 
            amount, LocalDate.now(), paymentReference);
        invoice.setNotes((invoice.getNotes() != null ? invoice.getNotes() + "\n" : "") + note);
        
        return invoiceRepository.save(invoice);
    }
}
