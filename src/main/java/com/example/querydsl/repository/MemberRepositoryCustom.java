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

	/**
	 * contents, count 쿼리 한 번에 요청
	 * @param condition
	 * @param pageable
	 * @return
	 */
	Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable);

	Page<MemberTeamDto> searchPageSimple_QuerydslRepositorySupport(MemberSearchCondition condition, Pageable pageable);

	/**
	 * count 쿼리 별도로 분리
	 * @param condition
	 * @param pageable
	 * @return
	 */
	Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable);

}
