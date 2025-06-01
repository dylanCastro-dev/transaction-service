package com.nttdata.transaction.model.Dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.nttdata.transaction.model.Details.CurrentAccount;
import com.nttdata.transaction.model.Details.FixedTermAccount;
import com.nttdata.transaction.model.Details.ProductDetails;
import com.nttdata.transaction.model.Details.SavingsAccount;
import com.nttdata.transaction.model.Details.CreditProduct;
import com.nttdata.transaction.model.Type.ProductStatus;
import com.nttdata.transaction.model.Type.ProductType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankProductDTO {
    private String id;
    private String customerId;
    private ProductType type;
    private ProductStatus status;
    private String name;
    private BigDecimal balance;
    private List<String> holders;
    private List<String> signers;

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
            property = "type",
            visible = true
    )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = SavingsAccount.class, name = "SAVINGS"),
            @JsonSubTypes.Type(value = FixedTermAccount.class, name = "FIXED_TERM"),
            @JsonSubTypes.Type(value = CurrentAccount.class, name = "CURRENT"),
            @JsonSubTypes.Type(value = CreditProduct.class, name = "CREDIT")
    })
    private ProductDetails details;
}
