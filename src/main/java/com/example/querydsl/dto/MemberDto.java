package com.example.querydsl.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@Getter
@NoArgsConstructor
public class MemberDto {
	private String username;
	private int age;

	public MemberDto(String username, int age) {
		this.username = username;
		this.age = age;
	}
}
