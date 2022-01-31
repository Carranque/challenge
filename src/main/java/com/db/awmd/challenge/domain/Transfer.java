package com.db.awmd.challenge.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Class used to get request information to perform a transfer between accounts
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Builder
@Data
public class Transfer {

    /**
     * Source account id
     */
    @NotNull
    @NotEmpty
    String sourceAccountId;

    /**
     * Destination account id
     */
    @NotNull
    @NotEmpty
    String destinationAccountId;

    /**
     * Amount to transfer
     */
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "Transfer amount must be greater than 0")
    BigDecimal amount;
}
