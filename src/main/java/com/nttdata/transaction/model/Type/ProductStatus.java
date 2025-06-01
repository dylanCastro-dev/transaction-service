package com.nttdata.transaction.model.Type;

/**
 * Enum que representa los posibles estados de un producto bancario.
 * Cada estado tiene un código asociado (String) y una descripción.
 */
public enum ProductStatus {

    /**
     * Producto activo: disponible para operaciones y en uso regular por el cliente.
     */
    ACTIVE,

    /**
     * Producto bloqueado por no cumplir con el monto promedio diario mensual requerido.
     */
    BLOCKED_AVG_BALANCE;
}
