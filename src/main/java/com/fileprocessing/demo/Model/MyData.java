package com.fileprocessing.demo.Model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MyData {
    private String name;
    private int age;

    // Constructors, getters, setters, and toString()
    public MyData() {}

    @JsonCreator
    public MyData(@JsonProperty("name") String name, @JsonProperty("age") Integer age) {
        this.name = name;
        this.age = age;
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

    @Override
    public String toString() {
        return "MyData{name='" + name + "', age=" + age + "}";
    }
}

