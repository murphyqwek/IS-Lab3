package com.example.is.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Coordinates {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private Double x;

    @NotNull
    @Column(nullable = false)
    private double y;

    public long getId() {
        return id;
    }

    public Double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

}