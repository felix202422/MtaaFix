package com.sms.entity;

import jakarta.persistence.*;


@Entity
@Table(name = "transport_students",
        uniqueConstraints = @UniqueConstraint(columnNames = {"transport_route_id", "student_id"}))
public class TransportStudent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transport_route_id", nullable = false)
    private TransportRoute transportRoute;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "pickup_point", length = 100)
    private String pickupPoint;

    public TransportStudent() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TransportRoute getTransportRoute() {
        return transportRoute;
    }

    public void setTransportRoute(TransportRoute transportRoute) {
        this.transportRoute = transportRoute;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public String getPickupPoint() {
        return pickupPoint;
    }

    public void setPickupPoint(String pickupPoint) {
        this.pickupPoint = pickupPoint;
    }

}
