package com.example.drawimagery;

public class Sigmoid {

    public static float sigmoid(float x) {
        return (float)(1 / (1 + Math.exp(-x)));
    }

    public static float sigmoid(float x, double x_weight) {
        return (float)(1 / (1 + Math.exp(-x_weight * x)));
    }

}
