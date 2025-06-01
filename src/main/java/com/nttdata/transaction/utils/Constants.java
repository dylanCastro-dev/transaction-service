package com.nttdata.transaction.utils;

public class Constants {

    public static final String SUCCESS_FIND_LIST_TRANSACTION =
            "Lista de transacciones obtenida correctamente.";
    public static final String SUCCESS_FIND_TRANSACTION =
            "Transacción obtenida correctamente.";
    public static final String ERROR_FIND_TRANSACTION =
            "No se encontró la transacción solicitada.";
    public static final String ERROR_FIND_PRODUCT =
            "No se encontró el producto solicitado.";
    public static final String ERROR_FIND_CUSTOMER =
            "No se encontró el cliente solicitado.";
    public static final String SUCCESS_CREATE_TRANSACTION =
            "Transacción registrada correctamente.";
    public static final String SUCCESS_UPDATE_TRANSACTION =
            "Transacción actualizada correctamente.";
    public static final String SUCCESS_DELETE_TRANSACTION =
            "Transacción eliminada correctamente.";
    public static final String SUCCESS_FIND_LIST_TRANSACTION_BY_PRODUCT =
            "Transacciones del producto obtenidas correctamente.";
    public static final String SUCCESS_GET_BALANCE =
            "Saldo disponible obtenido correctamente.";
    public static final String SUCCESS_APPLY_MONTHLY_FEE =
            "Tareas mensuales completadas correctamente.";
    public static final String ERROR_INTERNAL  =
            "Hubo un problema con la solicitud";
    public static final String ERROR_VALIDATION_MESSAGE =
            "Error de validación: %s";

    // Constantes para validaciones de reglas de transacción
    public static final String ERROR_UNSUPPORTED_TRANSACTION_TYPE =
            "Tipo de transacción no soportado.";
    public static final String ERROR_PAYMENT_ONLY_FOR_CREDIT_PRODUCTS =
            "Solo se pueden realizar pagos a productos de crédito.";
    public static final String ERROR_PAYMENT_EXCEEDS_DEBT =
            "El monto del pago excede la deuda actual.";
    public static final String ERROR_CREDIT_LIMIT_NOT_DEFINED =
            "Producto sin límite de crédito.";
    public static final String ERROR_AMOUNT_EXCEEDS_CREDIT_LIMIT =
            "Monto excede límite de crédito.";
    public static final String ERROR_INVALID_TRANSACTION_FOR_PRODUCT =
            "Transacción no válida para este tipo de producto.";
    public static final String ERROR_FIXED_TERM_WRONG_DAY =
            "Solo se permiten transacciones el día %d de cada mes para cuentas a plazo fijo.";
    public static final String ERROR_MONTHLY_LIMIT_REACHED =
            "Límite mensual de movimientos alcanzado para este producto.";
    public static final String ERROR_INSUFFICIENT_FUNDS =
            "Fondos insuficientes para el retiro.";
    public static final String ERROR_INVALID_TRANSACTION_FOR_BANK_ACCOUNT =
            "Tipo de transacción no válido para cuenta bancaria.";

}