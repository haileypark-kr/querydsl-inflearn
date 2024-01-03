package com.example.querydsl.repository;

import static com.example.querydsl.entity.QMember.*;
import static com.example.querydsl.entity.QTeam.*;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import com.example.querydsl.dto.MemberSearchCondition;
import com.example.querydsl.dto.MemberTeamDto;
import com.example.querydsl.dto.QMemberTeamDto;
import com.example.querydsl.entity.Member;
import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

/**
 * 사용자 정의 리포지토리 구현체
 */
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepositoryCustom {

	private final JPAQueryFactory jpaQueryFactory;

	@Override
	public List<MemberTeamDto> search(MemberSearchCondition condition) {

		return jpaQueryFactory
			.select(
				new QMemberTeamDto(member.id.as("memberId"),
					member.username,
					member.age,
					team.id.as("teamId"),
					team.name.as("teamName")
				))
			.from(member)
			.leftJoin(member.team, team)
			.where(
				usernameEq(condition.getUsername()),
				teamNameEq(condition.getTeamName()),
				ageGoe(condition.getAgeGoe()),
				ageLoe(condition.getAgeLoe())
			)
			.fetch();
	}

	/**
	 * spring data 의 페이징 기능을 활용한 단순한 방법
	 *
	 * @param condition
	 * @param pageable
	 * @return
	 */
	@Override
	public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
		QueryResults<MemberTeamDto> results = jpaQueryFactory
			.select(
				new QMemberTeamDto(member.id.as("memberId"),
					member.username,
					member.age,
					team.id.as("teamId"),
					team.name.as("teamName")
				))
			.from(member)
			.leftJoin(member.team, team)
			.where(
				usernameEq(condition.getUsername()),
				teamNameEq(condition.getTeamName()),
				ageGoe(condition.getAgeGoe()),
				ageLoe(condition.getAgeLoe())
			)
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetchResults();

		List<MemberTeamDto> content = results.getResults();
		long total = results.getTotal();

		return new PageImpl<>(content, pageable, total);
	}

	@Override
	public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
		List<MemberTeamDto> results = jpaQueryFactory
			.select(
				new QMemberTeamDto(member.id.as("memberId"),
					member.username,
					member.age,
					team.id.as("teamId"),
					team.name.as("teamName")
				))
			.from(member)
			.leftJoin(member.team, team)
			.where(
				usernameEq(condition.getUsername()),
				teamNameEq(condition.getTeamName()),
				ageGoe(condition.getAgeGoe()),
				ageLoe(condition.getAgeLoe())
			)
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

		JPAQuery<Member> countQuery = jpaQueryFactory
			.select(member)
			.from(member)
			.leftJoin(member.team, team)
			.where(
				usernameEq(condition.getUsername()),
				teamNameEq(condition.getTeamName()),
				ageGoe(condition.getAgeGoe()),
				ageLoe(condition.getAgeLoe())
			);

		return PageableExecutionUtils.getPage(results, pageable, countQuery::fetchCount);
	}

	private BooleanExpression usernameEq(String username) {
		return StringUtils.hasText(username) ? member.username.eq(username) : null;
	}

	private BooleanExpression teamNameEq(String teamName) {
		return StringUtils.hasText(teamName) ? team.name.eq(teamName) : null;
	}

	private BooleanExpression ageLoe(Integer ageLoe) {
		return ageLoe != null ? member.age.loe(ageLoe) : null;
	}

	private BooleanExpression ageGoe(Integer ageGoe) {
		return ageGoe != null ? member.age.goe(ageGoe) : null;
	}

}
