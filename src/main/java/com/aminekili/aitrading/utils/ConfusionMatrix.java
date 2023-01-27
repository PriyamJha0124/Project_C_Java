package com.aminekili.aitrading.utils;

public class ConfusionMatrix {

    int truePositive = 0;
    int trueNegative = 0;
    int falsePositive = 0;
    int falseNegative = 0;

    public ConfusionMatrix(String[] prediction, String[] actual) {
        for (int i = 0; i < prediction.length; i++) {
            if (prediction[i].equals("EXECUTE") && actual[i].equals("EXECUTE")) {
                truePositive++;
            } else if (prediction[i].equals("EXECUTE") && actual[i].equals("NO")) {
                falsePositive++;
            } else if (prediction[i].equals("NO") && actual[i].equals("EXECUTE")) {
                falseNegative++;
            } else if (prediction[i].equals("NO") && actual[i].equals("NO")) {
                trueNegative++;
            }
        }
    }


    public double getAccuracy() {
        return (truePositive + trueNegative) / (double) (truePositive + trueNegative + falsePositive + falseNegative);
    }

    public double getPrecision() {
        return truePositive / (double) (truePositive + falsePositive);
    }

    public double getRecall() {
        return truePositive / (double) (truePositive + falseNegative);
    }

    public double getF1Score() {
        return 2 * (getPrecision() * getRecall()) / (getPrecision() + getRecall());
    }

    public double getSpecificity() {
        return trueNegative / (double) (trueNegative + falsePositive);
    }

    public double getFalsePositiveRate() {
        return falsePositive / (double) (falsePositive + trueNegative);
    }

    public double getFalseNegativeRate() {
        return falseNegative / (double) (falseNegative + truePositive);
    }

    public double getTruePositiveRate() {
        return truePositive / (double) (truePositive + falseNegative);
    }

    public double getTrueNegativeRate() {
        return trueNegative / (double) (trueNegative + falsePositive);
    }

    public String toString() {
        return "ConfusionMatrix{" +
                "truePositive=" + truePositive +
                ", trueNegative=" + trueNegative +
                ", falsePositive=" + falsePositive +
                ", falseNegative=" + falseNegative +
                '}';
    }

}
