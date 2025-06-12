package com.nttdata.transaction.model.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardProductDTO {
    private String id;

    private String cardNumber;

    private String customerId;

    private String primaryAccountId;

    private List<String> linkedAccountIds;

    private boolean active;
}
