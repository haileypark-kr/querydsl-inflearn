package com.example.querydsl.dto;

import lombok.Data;

/**
 * 회원명, 팀명, 나이 - 검색 조건
 */
@Data
public class MemberSearchCondition {
	private String username;
	private String teamName;
	private Integer ageGoe; // 나이가 이 값보다 크거나 같음
	private Integer ageLoe; // 나이가 이 값보다 작거나 같음
}
