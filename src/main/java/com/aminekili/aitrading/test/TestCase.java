package com.aminekili.aitrading.test;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TestCase {

    private double volume;
    private double count;
    private double minute;
    private double wap;
    private double tesla3;
    private double tesla6;
    private double tesla9;
    private String decision;
    private String execute;

}
