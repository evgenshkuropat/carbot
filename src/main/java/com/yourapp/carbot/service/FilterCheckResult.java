package com.yourapp.carbot.service;

public record FilterCheckResult(
        boolean result,
        boolean carTypeOk,
        boolean brandOk,
        boolean maxPriceOk,
        boolean locationOk,
        boolean mileageOk,
        boolean fuelTypeOk,
        boolean transmissionOk,
        boolean yearOk
) {
}