package com.yourapp.carbot.bot;

import java.util.List;
import com.yourapp.carbot.entity.CarEntity;

public class SearchSession {

    private final List<CarEntity> cars;
    private int index;

    public SearchSession(List<CarEntity> cars) {
        this.cars = cars;
        this.index = 0;
    }

    public CarEntity current() {
        return cars.get(index);
    }

    public boolean hasNext() {
        return index < cars.size() - 1;
    }

    public boolean hasPrev() {
        return index > 0;
    }

    public void next() {
        if (hasNext()) {
            index++;
        }
    }

    public void prev() {
        if (hasPrev()) {
            index--;
        }
    }

    public int currentNumber() {
        return index + 1;
    }

    public int total() {
        return cars.size();
    }

    public boolean isEmpty() {
        return cars == null || cars.isEmpty();
    }
}