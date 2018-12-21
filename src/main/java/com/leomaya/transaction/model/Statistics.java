package com.leomaya.transaction.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Statistics {
    Double sum;
    Double avg;
    Double max;
    Double min;
    Long count;
}
