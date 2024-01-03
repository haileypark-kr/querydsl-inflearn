package com.example.querydsl.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.querydsl.dto.MemberSearchCondition;
import com.example.querydsl.dto.MemberTeamDto;

/**
 * 사용자 정의 리포지토리
 */
public interface MemberRepositoryCustom {
	List<MemberTeamDto> search(MemberSearchCondition condition);

	Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable);

	Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable);

}
