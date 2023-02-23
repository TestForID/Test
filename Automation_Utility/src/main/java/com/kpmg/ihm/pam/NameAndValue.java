package com.kpmg.ihm.pam;

public class NameAndValue {
    public NameAndValue() {
    }

    public NameAndValue(String name, String value) {
        this.name = name;
        this.value = value;
    }
    
    private String name;
    private String value;


    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}