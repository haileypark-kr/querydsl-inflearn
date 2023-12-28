package com.example.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@Getter
@NoArgsConstructor
public class MemberDto {
	private String username;
	private int age;

	@QueryProjection // 이 어노테이션이 있으면 이 DTO로 Q파일을 만들어준다.
	public MemberDto(String username, int age) {
		this.username = username;
		this.age = age;
	}

}
