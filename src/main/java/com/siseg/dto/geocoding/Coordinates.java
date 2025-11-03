package com.siseg.dto.geocoding;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Coordinates {
    private BigDecimal latitude;
    private BigDecimal longitude;
}

