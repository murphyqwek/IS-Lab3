package com.example.is.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private Color eyeColor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private Color hairColor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = true)
    private Location location;

    @Positive
    @Column(
            nullable = true,
            check = @CheckConstraint(
                    name = "chk_person_weight_positive",
                    constraint = "weight > 0"))
    private Float weight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private Country nationality;

    public long getId() {
        return id;
    }

    public Color getEyeColor() {
        return eyeColor;
    }

    public void setEyeColor(Color eyeColor) {
        this.eyeColor = eyeColor;
    }

    public Color getHairColor() {
        return hairColor;
    }

    public void setHairColor(Color hairColor) {
        this.hairColor = hairColor;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Float getWeight() {
        return weight;
    }

    public void setWeight(Float weight) {
        this.weight = weight;
    }

    public Country getNationality() {
        return nationality;
    }

    public void setNationality(Country nationality) {
        this.nationality = nationality;
    }
}