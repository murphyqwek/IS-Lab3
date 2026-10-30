package com.example.is.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    @Column(
            nullable = false,
            check = @CheckConstraint(
                    name = "check_ticket_name_not_blank",
                    constraint = "char_length(trim(name)) > 0"
            )
    )
    private String name;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private Coordinates coordinates;

    @CreationTimestamp
    @Column(
            nullable = false,
            updatable = false
    )
    private java.time.ZonedDateTime creationDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = true)
    private Person person;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private Event event;

    @Positive
    @Column(
            nullable = false,
            check = @CheckConstraint(
                    name = "chk_ticket_price_positive",
                    constraint = "price > 0"
            )
    )
    private int price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private TicketType type;

    @Min(1)
    @Max(100)
    @Column(
            nullable = false,
            check = @CheckConstraint(
                    name = "check_ticket_discount_range",
                    constraint = "discount >= 1 AND discount <= 100"
            )
    )
    private int discount;

    @Positive
    @Column(
            nullable = true,
            check = @CheckConstraint(
                    name = "check_ticket_number_positive",
                    constraint = "number > 0"
            )
    )
    private Float number;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = true)
    private Venue venue;

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
    }

    public ZonedDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(ZonedDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public TicketType getType() {
        return type;
    }

    public void setType(TicketType type) {
        this.type = type;
    }

    public int getDiscount() {
        return discount;
    }

    public void setDiscount(int discount) {
        this.discount = discount;
    }

    public Float getNumber() {
        return number;
    }

    public void setNumber(Float number) {
        this.number = number;
    }

    public Venue getVenue() {
        return venue;
    }

    public void setVenue(Venue venue) {
        this.venue = venue;
    }
}
