package com.railway.reservation.model;

import jakarta.persistence.*;

@Entity
@Table(name = "passengers")
public class Passenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long passengerId;

    private String name;

    private int age;

    private String gender;

    private String category;

    private String phone;

    private String email;

    private String pnr;

    // Default constructor
    public Passenger() {
    }

    // Constructor
    public Passenger(String name, int age, String gender,
                     String category, String phone, String email) {
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.category = category;
        this.phone = phone;
        this.email = email;
        this.pnr = null;
    }

    // Getters and Setters

    public Long getPassengerId() {
        return passengerId;
    }

    public void setPassengerId(Long passengerId) {
        this.passengerId = passengerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPnr() {
        return pnr;
    }

    public void setPnr(String pnr) {
        this.pnr = pnr;
    }

    @Override
    public String toString() {
        return "Passenger{" +
                "passengerId=" + passengerId +
                ", name='" + name + '\'' +
                ", age=" + age +
                ", gender='" + gender + '\'' +
                ", category='" + category + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", pnr='" + pnr + '\'' +
                '}';
    }
}