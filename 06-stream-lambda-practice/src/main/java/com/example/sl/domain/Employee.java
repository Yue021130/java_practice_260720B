package com.example.sl.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

    private Long id;
    private String name;
    private Integer age;
    private String dept;
    private Integer salary;
    private String gender;
    private LocalDate joinDate;
}
