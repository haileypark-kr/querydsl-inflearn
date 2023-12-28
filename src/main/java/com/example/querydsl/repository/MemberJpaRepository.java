package com.example.querydsl.repository;

import static com.example.querydsl.entity.QMember.*;
import static com.example.querydsl.entity.QTeam.*;

import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.example.querydsl.dto.MemberSearchCondition;
import com.example.querydsl.dto.MemberTeamDto;
import com.example.querydsl.dto.QMemberTeamDto;
import com.example.querydsl.entity.Member;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

@Repository
public class MemberJpaRepository {
	private final EntityManager em; // 순수 JPA 사용하려면 entity manager 필요
	private final JPAQueryFactory queryFactory; // querydsl 사용하려면 필요

	public MemberJpaRepository(EntityManager em, JPAQueryFactory queryFactory) {
		this.em = em;
		// this.queryFactory = new JPAQueryFactory(em); // em이 동시성 문제 없기 때문에 이렇게 사용해도 됨
		this.queryFactory = queryFactory; // JPAQueryFactoryConfig에서 등록한 JPAQueryFactory bean 사용
	}

	public void save(Member member) {
		em.persist(member);
	}

	public Optional<Member> findById(Long id) {
		return Optional.ofNullable(em.find(Member.class, id));
	}

	public List<Member> findAll() {
		return em.createQuery("select m from Member m", Member.class).getResultList();
	}

	public List<Member> findAll_QueryDSL() {
		return queryFactory.selectFrom(member).fetch();
	}

	public List<Member> findByUsername(String username) {
		return em.createQuery("select m from Member m where m.username = :username", Member.class)
			.setParameter("username", username)
			.getResultList();
	}

	public List<Member> findByUsername_QueryDSL(String username) {
		return queryFactory
			.selectFrom(member)
			.where(member.username.eq(username))
			.fetch();
	}

	/**
	 * BooleanBuilder를 가지고 멤버 검색
	 * @param condition
	 * @return
	 */
	public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {

		BooleanBuilder builder = new BooleanBuilder();
		if (StringUtils.hasText(condition.getUsername())) {
			builder.and(member.username.eq(condition.getUsername()));
		}

		if (StringUtils.hasText(condition.getTeamName())) {
			builder.and(team.name.eq(condition.getTeamName()));
		}

		if (condition.getAgeGoe() != null) {
			builder.and(member.age.goe(condition.getAgeGoe()));
		}

		if (condition.getAgeLoe() != null) {
			builder.and(member.age.loe(condition.getAgeLoe()));
		}

		return queryFactory
			.select(new QMemberTeamDto(member.id.as("memberId"),
				member.username,
				member.age,
				team.id.as("teamId"),
				team.name.as("teamName")
			))
			.from(member)
			.leftJoin(member.team, team) // team 정보도 가져와야 하기 때문에 조인을 한다.
			.where(builder)
			.fetch();
	}

	/**
	 * where 여러 파라미터로 검색
	 * @param condition
	 * @return
	 */
	public List<MemberTeamDto> search(MemberSearchCondition condition) {

		return queryFactory
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

	private BooleanExpression usernameEq(String username) {
		return StringUtils.hasText(username) ? member.username.eq(username) : null;
	}

	private BooleanExpression teamNameEq(String teamName) {
		return StringUtils.hasText(teamName) ? team.name.eq(teamName) : null;
	}

	private BooleanExpression ageGoe(Integer ageGoe) {
		return ageGoe != null ? member.age.goe(ageGoe) : null;
	}

	private BooleanExpression ageLoe(Integer ageLoe) {
		return ageLoe != null ? member.age.goe(ageLoe) : null;
	}

	/**
	 * 이렇게 BooleanExpression 조립 가능.
	 * @param ageLoe
	 * @param ageGoe
	 * @return
	 */
	private BooleanExpression ageBetween(int ageLoe, int ageGoe) {
		// null 체크 조심.
		return ageGoe(ageGoe).and(ageLoe(ageLoe));
	}
}
