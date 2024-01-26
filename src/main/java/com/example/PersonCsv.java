package com.example;

public record PersonCsv(
    String person_ID,
    String name,
    String first,
    String last,
    String middle,
    String email,
    String phone,
    String fax,
    String title
) {
}