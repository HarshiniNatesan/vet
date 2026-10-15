package com.vet.model;

/** Method used by the payer for a payment that staff have confirmed as received. */
public enum PaymentMethod {
    CASH("Cash"), UPI("UPI"), CARD("Card"), BANK_TRANSFER("Bank Transfer"), OTHER("Other");

    private final String label;
    PaymentMethod(String label) { this.label = label; }
    public String getLabel() { return label; }
}
