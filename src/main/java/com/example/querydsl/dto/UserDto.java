package com.example.querydsl.dto;

import lombok.Data;

@Data
public class UserDto {
	private String name; // username이 아닌 name 사용
	private int age;

	public UserDto(String name, int age) {
		this.name = name;
		this.age = age;
	}
}
