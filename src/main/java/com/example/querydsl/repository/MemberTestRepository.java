package com.example.querydsl.repository;

import static com.example.querydsl.entity.QMember.*;
import static com.example.querydsl.entity.QTeam.*;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.example.querydsl.dto.MemberSearchCondition;
import com.example.querydsl.entity.Member;
import com.example.querydsl.repository.support.Querydsl4RepositorySupport;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;

/**
 * Querydsl4RepositorySupport 사용하는 방법 예시 코드.
 */
@Repository
public class MemberTestRepository extends Querydsl4RepositorySupport {

	public MemberTestRepository() {
		super(Member.class);
	}

	public List<Member> basicSelect() {
		// Querydsl4RepositorySupport의 select를 사용하기 때문에 jpaQueryFactory 없이 바로 select부터 사용 가능.
		return select(member).from(member).fetch();
	}

	public List<Member> basicSelectFrom() {
		// Querydsl4RepositorySupport의 selectFrom을 사용하기 때문에 jpaQueryFactory 없이 바로 selectFrom부터 사용 가능.
		return selectFrom(member).fetch();
	}

	/**
	 * querydsl의 applyPagination과 PageableExecutionUtils을 활용한 페이징 최적화 쿼리.
	 * @param condition
	 * @param pageable
	 * @return
	 */
	public Page<Member> searchPageByApplyPage(MemberSearchCondition condition, Pageable pageable) {
		JPAQuery<Member> jpaQuery = selectFrom(member)
			.leftJoin(member.team, team)
			.where(
				usernameEq(condition.getUsername()),
				teamNameEq(condition.getTeamName()),
				ageGoe(condition.getAgeGoe()),
				ageLoe(condition.getAgeLoe())
			);
		List<Member> content = getQuerydsl().applyPagination(pageable, jpaQuery).fetch();

		return PageableExecutionUtils.getPage(content, pageable, jpaQuery::fetchCount);
	}

	/**
	 * Querydsl4RepositorySupport 를 사용하여 페이지네이션.
	 * 위의 searchPageByApplyPage와 동일한 코드이다.
	 * @param condition
	 * @param pageable
	 * @return
	 */
	public Page<Member> applyPagination(MemberSearchCondition condition, Pageable pageable) {
		return applyPagination(pageable, queryFactory -> queryFactory
			.selectFrom(member)
			.leftJoin(member.team, team)
			.where(
				usernameEq(condition.getUsername()),
				teamNameEq(condition.getTeamName()),
				ageGoe(condition.getAgeGoe()),
				ageLoe(condition.getAgeLoe())
			)
		);
	}

	/**
	 * contents 조회 쿼리와 count 조회 쿼리를 분리하여 사용
	 * @param condition
	 * @param pageable
	 * @return
	 */
	public Page<Member> applyPagination2(MemberSearchCondition condition, Pageable pageable) {
		return applyPagination(pageable,
			// contents 용 쿼리
			contentsQueryFactory -> contentsQueryFactory
				.selectFrom(member)
				.leftJoin(member.team, team)
				.where(
					usernameEq(condition.getUsername()),
					teamNameEq(condition.getTeamName()),
					ageGoe(condition.getAgeGoe()),
					ageLoe(condition.getAgeLoe())
				),
			// count 용 쿼리
			countQueryFactory -> countQueryFactory
				.select(member.id)
				.from(member)
				.where(
					usernameEq(condition.getUsername()),
					teamNameEq(condition.getTeamName()),
					ageGoe(condition.getAgeGoe()),
					ageLoe(condition.getAgeLoe())
				)
		);
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
