package com.example.querydsl;

import static com.example.querydsl.entity.QMember.*;
import static com.example.querydsl.entity.QTeam.*;
import static org.assertj.core.api.Assertions.*;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.querydsl.dto.MemberDto;
import com.example.querydsl.dto.QMemberDto;
import com.example.querydsl.dto.UserDto;
import com.example.querydsl.entity.Member;
import com.example.querydsl.entity.QMember;
import com.example.querydsl.entity.Team;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

@Transactional
@SpringBootTest
public class QueryDslBasicTest {

	@Autowired
	EntityManager em;

	JPAQueryFactory queryFactory;

	@BeforeEach
	private void before() {
		queryFactory = new JPAQueryFactory(em);

		Team teamA = Team.builder().name("teamA").build();
		Team teamB = Team.builder().name("teamB").build();
		em.persist(teamA);
		em.persist(teamB);

		Member member1 = Member.builder().username("member1").age(10).team(teamA).build();
		Member member2 = Member.builder().username("member2").age(20).team(teamA).build();
		Member member3 = Member.builder().username("member3").age(30).team(teamB).build();
		Member member4 = Member.builder().username("member4").age(40).team(teamB).build();
		em.persist(member1);
		em.persist(member2);
		em.persist(member3);
		em.persist(member4);
	}

	@Test
	public void startJPQL() throws Exception {
		// 1. 멤버1 을 찾아라
		Member findMember = em.createQuery("select m from Member m  where m.username = :username", Member.class)
			.setParameter("username", "member1")
			.getSingleResult();

		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	@Transactional
	public void startQueryDsl() throws Exception {
		Member findMember = queryFactory
			.select(member)
			.from(member)
			.where(member.username.eq("member1"))
			.fetchOne();

		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	@Transactional
	public void search() throws Exception {
		// queryFactory.selectFrom(member) ==> select(member).from(member)와 동일.
		Member findMember = queryFactory.selectFrom(member)
			.where(member.username.eq("member1")
				.and(member.age.eq(10)))
			.fetchOne();

		assertThat(findMember.getUsername()).isEqualTo("member1");
		assertThat(findMember.getAge()).isEqualTo(10);

	}

	@Test
	@Transactional
	public void searchAndParam() throws Exception {
		Member findMember = queryFactory.selectFrom(member)
			.where(member.username.eq("member1"), member.age.eq(10)) // 여러 개를 ... 으로 넘기면 다 and 조건 가능.
			.fetchOne();

		assertThat(findMember.getUsername()).isEqualTo("member1");
		assertThat(findMember.getAge()).isEqualTo(10);
	}

	@Test
	@Transactional
	public void resultFetchTest() throws Exception {
		// List<Member> members = queryFactory.selectFrom(member).fetch(); // 리스트 조회
		// Member memberOne = queryFactory.selectFrom(member).fetchOne(); // 단건 조회
		// Member memberFirst = queryFactory.selectFrom(member).fetchFirst(); // limit(1).fetchOne()
		// QueryResults<Member> results = queryFactory.selectFrom(member).fetchResults(); // 페이징

		queryFactory.selectFrom(member).fetchCount(); // count 쿼리만 나감
	}

	/**
	 * 회원 정렬
	 * 1. 나이 내림차순
	 * 2. 이름 올림차순
	 * 3. 2에서 이름이 없으면 출력 (nulls last)
	 * @throws Exception
	 */
	@Test
	@Transactional
	public void sort() throws Exception {
		Member member1 = Member.builder().username(null).age(100).build();
		Member member2 = Member.builder().username("member5").age(100).build();
		Member member3 = Member.builder().username("member6").age(100).build();
		em.persist(member1);
		em.persist(member2);
		em.persist(member3);

		List<Member> result = queryFactory.selectFrom(member)
			.where(member.age.eq(100))
			.orderBy(member.age.desc(), member.username.asc().nullsLast())
			.fetch();

		Member member5 = result.get(0);
		Member member6 = result.get(1);
		Member member7 = result.get(2);

		assertThat(member5.getUsername()).isEqualTo("member5");
		assertThat(member6.getUsername()).isEqualTo("member6");
		assertThat(member7.getUsername()).isNull();
	}

	@Test
	@Transactional
	public void paging() throws Exception {
		List<Member> result = queryFactory.selectFrom(member)
			.orderBy(member.username.desc())
			.offset(1) // 0부터 시작. 1은 앞에 한개 스킵
			.limit(2)
			.fetch();

		assertThat(result.size()).isEqualTo(2);

		// 전체 조회
		QueryResults<Member> queryResults = queryFactory.selectFrom(member)
			.orderBy(member.username.desc())
			.offset(1) // 0부터 시작. 1은 앞에 한개 스킵
			.limit(2)
			.fetchResults();
		assertThat(queryResults.getTotal()).isEqualTo(4); // 페이징(offset, limit) 적용 안한 전체 count
		assertThat(queryResults.getLimit()).isEqualTo(2); // 내가 설정했던 limit
		assertThat(queryResults.getOffset()).isEqualTo(1); // 내가 설정했던 offset
		assertThat(queryResults.getResults().size()).isEqualTo(2); // 페이징 결과
	}

	@Test
	@Transactional
	public void aggregation() throws Exception {
		List<Tuple> result = queryFactory.select(member.count(),
				member.age.sum(),
				member.age.avg(),
				member.age.max(),
				member.age.min())
			.from(member)
			.fetch(); // 결과는 QueryDSL의 Tuple 로 리턴됨.

		Tuple tuple = result.get(0);
		assertThat(tuple.get(member.count())).isEqualTo(4); // tuple에서 조회할 때 위에서 조회했던 컬럼(?)필드를 그대로 가져옴.
		assertThat(tuple.get(member.age.sum())).isEqualTo(100);
		assertThat(tuple.get(member.age.avg())).isEqualTo(25);
	}

	/**
	 * 팀 이름과 각 팀의 평균 연령 (팀 이름으로 grouping)
	 * @throws Exception
	 */
	@Test
	@Transactional
	public void group() throws Exception {
		List<Tuple> result = queryFactory.select(team.name, member.age.avg())
			.from(member)
			.join(member.team, team)
			.groupBy(team.name)
			.fetch();

		Tuple teamA = result.get(0);
		Tuple teamB = result.get(1);

		assertThat(teamA.get(team.name)).isEqualTo("teamA");
		assertThat(teamB.get(team.name)).isEqualTo("teamB");

		assertThat(teamA.get(member.age.avg())).isEqualTo(15);
		assertThat(teamB.get(member.age.avg())).isEqualTo(35);
	}

	/**
	 * 팀A에 소속된 모든 회원을 찾아라
	 * @throws Exception
	 */
	@Test
	@Transactional
	public void join() throws Exception {

		// inner join
		List<Member> result = queryFactory.selectFrom(member)
			.join(member.team, team) // .innerjoin() 과 동일.
			.where(team.name.eq("teamA"))
			.fetch();

		assertThat(result).extracting("username").containsExactly("member1", "member2");

		// left join
		List<Member> leftJoinResult = queryFactory.selectFrom(member)
			.leftJoin(member.team, team) // left outer join
			.where(team.name.eq("teamA"))
			.fetch();

		assertThat(leftJoinResult).extracting("username").containsExactly("member1", "member2");
	}

	/**
	 * theta join (연관관계가 없어도 join 가능)
	 * @throws Exception
	 */
	@Test
	@Transactional
	public void thetaJoin() throws Exception {
		em.persist(Member.builder().username("teamA").build());
		em.persist(Member.builder().username("teamB").build());

		List<Member> result = queryFactory
			.select(member)
			.from(member, team) // from 절에 조인할 엔티티 목록 쓰면 됨.
			.where(member.username.eq(team.name))
			.fetch();

		assertThat(result).extracting("username").containsExactly("teamA", "teamB");
	}

	/**
	 * 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회(left join)
	 * JPQL: select m, t from Member m left join m.team t on t.name = 'teamA'
	 * @throws Exception
	 */
	@Test
	@Transactional
	public void joinOnFiltering() throws Exception {
		List<Tuple> result = queryFactory.select(member, team) // select가 여러개이기 때문에 Tuple로 나옴
			.from(member)
			.leftJoin(member.team, team)
			.on(team.name.eq("teamA"))
			.fetch();

		for (Tuple tuple : result) {
			System.out.println(tuple);
		}

		List<Tuple> result2 = queryFactory.select(member, team) // select가 여러개이기 때문에 Tuple로 나옴
			.from(member)
			.leftJoin(member.team, team)
			.where(team.name.eq("teamA"))
			.fetch();

		for (Tuple tuple : result2) {
			System.out.println(tuple);
		}
	}

	/**
	 * 연관관계가 없는 엔티티 외부 조인
	 * @throws Exception
	 */
	@Test
	@Transactional
	public void joinOnNoRelation() throws Exception {
		em.persist(Member.builder().username("teamA").build());
		em.persist(Member.builder().username("teamB").build());

		List<Tuple> result = queryFactory
			.select(member, team)
			.from(member)
			.leftJoin(team).on(member.username.eq(team.name)) // ==> ID가 아닌 on 절에 있는 것으로 join
			// .leftJoin(member.team, team) // ==> 이게 보통의 join. ID 기반 join
			.fetch();

		for (Tuple tuple : result) {
			System.out.println(tuple);
		}
	}

	@PersistenceUnit
	EntityManagerFactory emf;

	/**
	 * fetch join이 없을 때.
	 * @throws Exception
	 */
	@Test
	@Transactional
	public void fetchJoinNo() throws Exception {
		em.flush();
		em.clear();

		Member member1 = queryFactory
			.selectFrom(member)
			.where(member.username.eq("member1"))
			.fetchOne();

		// persistence context에 로딩이 되었는지 안되었는지 알려줌. 지금은 false가 나와야함.
		boolean loaded = emf.getPersistenceUnitUtil().isLoaded(member1.getTeam());
		assertThat(loaded).as("fetch join 미적용").isFalse();
	}

	@Test
	@Transactional
	public void fetchJoinUsed() throws Exception {
		em.flush();
		em.clear();

		Member member1 = queryFactory
			.selectFrom(member)
			.join(member.team, team)
			.fetchJoin() // member를 조회할 때, 연관된 team을 한 번에 조회한다. join이든 left join이든 .fetchJoin() 한 번만 붙여주면 됨.
			.where(member.username.eq("member1"))
			.fetchOne();

		// persistence context에 로딩이 되었는지 안되었는지 알려줌. 지금은 false가 나와야함.
		boolean loaded = emf.getPersistenceUnitUtil().isLoaded(member1.getTeam());
		assertThat(loaded).as("fetch join 적용").isTrue();
	}

	/**
	 * 나이가 가장 많은 회원을 조회
	 * @throws Exception
	 */
	@Test
	@Transactional
	public void subQuery() throws Exception {

		QMember memberSub = new QMember("memberSub"); // 밖의 member와 sub query 안의 member는 겹치면 안됨.

		List<Member> result = queryFactory
			.selectFrom(member)
			.where(
				member.age.eq(
					JPAExpressions
						.select(memberSub.age.max())
						.from(memberSub)
				)
			).fetch();

		assertThat(result).extracting("age").containsExactly(40);
	}

	/**
	 * 나이가 평균 이상인 많은 회원을 조회
	 * @throws Exception
	 */
	@Test
	@Transactional
	public void subQuery_goe() throws Exception {

		QMember memberSub = new QMember("memberSub"); // 밖의 member와 sub query 안의 member는 겹치면 안됨.

		List<Member> result = queryFactory
			.selectFrom(member)
			.where(
				member.age.goe(
					JPAExpressions
						.select(memberSub.age.avg())
						.from(memberSub)
				)
			).fetch();

		assertThat(result).extracting("age").containsExactly(30, 40);
	}

	/**
	 * select 절에 subquery 사용
	 * @throws Exception
	 */
	@Test
	@Transactional
	public void subQuery_select() throws Exception {

		QMember memberSub = new QMember("memberSub"); // 밖의 member와 sub query 안의 member는 겹치면 안됨.

		List<Member> result = queryFactory
			.selectFrom(member)
			.where(
				member.age.in(
					JPAExpressions
						.select(memberSub.age)
						.from(memberSub)
						.where(memberSub.age.gt(10))
				)
			).fetch();

		assertThat(result).extracting("age").containsExactly(20, 30, 40);
	}

	@Test
	@Transactional
	public void subQuery_in() throws Exception {
		QMember memberSub = new QMember("memberSub"); // 밖의 member와 sub query 안의 member는 겹치면 안됨.

		List<Tuple> tuples = queryFactory
			.select(member.username,
				JPAExpressions
					.select(memberSub.age.avg()).from(memberSub))
			.from(member)
			.fetch();

		for (Tuple tuple : tuples) {
			System.out.println(tuple);
		}
	}

	/**
	 * select case 문 - 간단
	 * @throws Exception
	 */
	@Test
	@Transactional
	public void basicCase() throws Exception {
		List<String> result = queryFactory
			.select(member.age
				.when(10).then("열살")
				.when(20).then("스무살")
				.otherwise("기타")
			)
			.from(member)
			.fetch();

		for (String s : result) {
			System.out.println(s);
		}
	}

	/**
	 * select case 문 - 복잡한 쿼리는 CaseBuilder 사용.
	 * @throws Exception
	 */
	@Test
	@Transactional
	public void complexCase_caseBuilder() throws Exception {
		List<String> result = queryFactory
			.select(new CaseBuilder()
				.when(member.age.between(0, 20)).then("0~20")
				.when(member.age.between(21, 30)).then("21~30")
				.otherwise("기타")
			)
			.from(member)
			.fetch();

		for (String s : result) {
			System.out.println(s);
		}
	}

	/**
	 * select에서 상수를 리턴해야할 경우
	 * @throws Exception
	 */
	@Test
	@Transactional
	public void constant() throws Exception {
		List<Tuple> result = queryFactory
			.select(member.username, Expressions.constant("A")) // querydsl의 Expressions.constant 사용하라
			.from(member)
			.fetch();

		for (Tuple tuple : result) {
			System.out.println(tuple);
		}
	}

	/**
	 * 문자열을 concat해야 하는 경우
	 * @throws Exception
	 */
	@Test
	@Transactional
	public void string_concat() throws Exception {

		// {username}_{age} 이렇게 만들고 싶다.
		// 근데 select(member.username.concat("_").concat(member.age)) ==> 은 string + int 라서 concat이 안됨.
		// 문자가 아닌 다른 타입들은 .stringValue() 쓰면 됨 (enum도!)
		List<String> result = queryFactory
			.select(member.username.concat("_").concat(member.age.stringValue())) // .stringValue() 붙여주기.
			.from(member)
			.fetch();

		for (String s : result) {
			System.out.println(s);
		}
	}

	@Test
	@Transactional
	public void projection_one() throws Exception {
		List<String> result = queryFactory
			.select(member.username)
			.from(member)
			.fetch();
		for (String s : result) {
			System.out.println(s);
		}
		List<Member> members = queryFactory
			.select(member)
			.from(member)
			.fetch();
		for (Member s : members) {
			System.out.println(s);
		}
	}

	@Test
	@Transactional
	public void projection_tuple() throws Exception {

		List<Tuple> result = queryFactory
			.select(member.username, member.age)
			.from(member)
			.fetch();

		for (Tuple tuple : result) {
			System.out.println("username = " + tuple.get(member.username));
			System.out.println("age = " + tuple.get(member.age));
		}
	}

	@Test
	@Transactional
	public void findDtoByJPQL() throws Exception {
		List<MemberDto> resultList = em.createQuery("select new com.example.querydsl.dto.MemberDto(m.username, m.age)"
			+ " from Member m", MemberDto.class).getResultList();

		for (MemberDto memberDto : resultList) {
			System.out.println(memberDto);
		}
	}

	@Test
	@Transactional
	public void findDtoByQueryDSL_프로퍼티세터접근() throws Exception {
		// 기본 생성자와 setter가 있어야 함
		List<MemberDto> result = queryFactory
			.select(Projections.bean(MemberDto.class, member.username, member.age))
			.from(member)
			.fetch();

		for (MemberDto memberDto : result) {
			System.out.println(memberDto);
		}
	}

	@Test
	@Transactional
	public void findDtoByQueryDSL_필드직접접근() throws Exception {
		// setter 없어도 됨. 기본 생성자만 있으면 됨
		List<MemberDto> result = queryFactory
			.select(Projections.fields(MemberDto.class, member.username, member.age))
			.from(member)
			.fetch();

		for (MemberDto memberDto : result) {
			System.out.println(memberDto);
		}
	}

	@Test
	@Transactional
	public void findDtoByQueryDSL_생성자() throws Exception {
		// constructor에서 타입과 순서를 맞춰야 함.
		List<MemberDto> result = queryFactory
			.select(Projections.constructor(MemberDto.class, member.username, member.age))
			.from(member)
			.fetch();

		for (MemberDto memberDto : result) {
			System.out.println(memberDto);
		}
	}

	@Test
	@Transactional
	public void findUserDtoByQueryDSL_필드직접접근() throws Exception {
		List<UserDto> result = queryFactory
			.select(Projections.fields(UserDto.class, member.username, member.age))
			.from(member)
			.fetch();

		for (UserDto u : result) {
			System.out.println(u);
		}

		// 필드 뒤에 .as(별칭) 사용하면 됨.
		List<UserDto> resultAs = queryFactory
			.select(Projections.fields(UserDto.class, member.username.as("name"), member.age))
			.from(member)
			.fetch();

		for (UserDto u : resultAs) {
			System.out.println(u);
		}
	}

	@Test
	@Transactional
	public void findUserDtoByQueryDSL_필드직접접근_서브쿼리() throws Exception {
		// 서브쿼리 별칭 만들 때는 ExpresstionUtils.as 쓰면 됨.
		QMember memberSub = new QMember("memberSub");
		List<UserDto> resultSubAs = queryFactory
			.select(Projections.fields(UserDto.class,
				member.username.as("name"),
				ExpressionUtils.as(JPAExpressions.select(memberSub.age.max()).from(memberSub), "age")
			))
			.from(member)
			.fetch();

		for (UserDto u : resultSubAs) {
			System.out.println(u);
		}
	}

	@Test
	@Transactional
	public void findUserDtoByQueryDSL_생성자() throws Exception {
		List<UserDto> resultAs = queryFactory
			.select(Projections.constructor(UserDto.class, member.username, member.age))
			// .select(Projections.constructor(UserDto.class, member.username, member.age, member.id)) // 컴파일 오류 안뜸.
			.from(member)
			.fetch();

		for (UserDto u : resultAs) {
			System.out.println(u);
		}
	}

	/**
	 * 프로젝션 - DTO 생성자에 @QueryProjection 어노테이션 붙여서 사용
	 * @throws Exception
	 */
	@Test
	@Transactional
	public void findDtoByQueryProjection() throws Exception {
		List<MemberDto> result = queryFactory
			.select(new QMemberDto(member.username, member.age))
			// .select(new QMemberDto(member.username, member.age, member.id)) // 컴파일 오류
			.from(member)
			.fetch();
		for (MemberDto u : result) {
			System.out.println(u);
		}
	}

	/**
	 * 동적 쿼리 사용 - BooleanBuilder
	 * @throws Exception
	 */
	@Test
	@Transactional
	public void dynamicQuery_BooleanBuilder() throws Exception {
		String usernameParam = "member1";
		Integer ageParam = 10;

		List<Member> result = searchMember1(usernameParam, ageParam);
		assertThat(result.size()).isEqualTo(1);
	}

	private List<Member> searchMember1(String usernameCond, Integer ageCond) {

		BooleanBuilder builder = new BooleanBuilder();
		if (usernameCond != null) {
			builder.and(member.username.eq(usernameCond));
		}
		if (ageCond != null) {
			builder.and(member.age.eq(ageCond));
		}

		return queryFactory
			.selectFrom(member)
			.where(builder)
			.fetch();
	}

	/**
	 * 동적 쿼리 사용 - where 다중 파라미터 사용
	 * @throws Exception
	 */
	@Test
	@Transactional
	public void dynamicQuery_where() throws Exception {

		String usernameParam = "member1";
		Integer ageParam = null;

		List<Member> result = searchMember2(usernameParam, ageParam);
		assertThat(result.size()).isEqualTo(1);
	}

	private List<Member> searchMember2(String usernameCond, Integer ageCond) {
		return queryFactory
			.selectFrom(member)
			.where(usernameEq(usernameCond), ageEq(ageCond))
			.fetch();
	}

	private BooleanExpression usernameEq(String usernameCond) {
		return usernameCond != null ? member.username.eq(usernameCond) : null;
	}

	private BooleanExpression ageEq(Integer ageCond) {
		return ageCond != null ? member.age.eq(ageCond) : null;
	}

	/**
	 * 동적쿼리 where 다중 파라미터의 엄청난 장점 - 메소드 조합이 가능.
	 * @param usernameCond
	 * @param ageCond
	 * @return
	 */
	private BooleanExpression allEq(String usernameCond, Integer ageCond) {
		return usernameEq(usernameCond).and(ageEq(ageCond));
	}

	@Test
	@Transactional
	public void bulkUpdate() throws Exception {

		// 영속성 컨텍스트
		// member1 10 => member1
		// member2 20 => member2
		// member3 30 => member3
		// member4 40 => member4

		// DB
		// member1 10 => 비회원
		// member2 20 => 비회원
		// member3 30 => 회원
		// member4 40 => 회원

		long count = queryFactory
			.update(member)
			.set(member.username, "비회원")
			.where(member.age.lt(28))
			.execute();

		// 벌크 쿼리를 날리면 DB와 영속성 컨텍스트가 무조건 다르기 때문에 flush, clear 해주기.
		em.flush();
		em.clear();

		// 아래 쿼리는 영속성 컨텍스트에서 가져오므로, DB에 있는 값과 다른 값을 가져온다.
		List<Member> result = queryFactory
			.selectFrom(member)
			.fetch();

		for (Member u : result) {
			System.out.println(u);
		}
	}

	@Test
	@Transactional
	public void bulkUpdate_Add() throws Exception {
		queryFactory
			.update(member)
			.set(member.age, member.age.add(1))
			.execute();
		queryFactory
			.update(member)
			.set(member.age, member.age.multiply(2))
			.execute();
	}

	@Test
	@Transactional
	public void bulkDelete() throws Exception {
		queryFactory
			.delete(member)
			.where(member.age.gt(18))
			.execute();
	}

	/**
	 * SQL 함수 호출 - replace
	 * @throws Exception
	 */
	@Test
	@Transactional
	public void sqlFunction_replace() throws Exception {

		// member라는 단어를 M으로 치환하여 조회하기
		List<String> result = queryFactory
			.select(
				Expressions.stringTemplate("function('replace', {0}, {1}, {2})",
					member.username, "member", "M")
			)
			.from(member)
			.fetch();

		for (String u : result) {
			System.out.println(u);
		}
	}

	/**
	 * SQL 함수 호출 - lower
	 * @throws Exception
	 */
	@Test
	@Transactional
	public void sqlFunction_lower() throws Exception {

		// 소문자로 변경했을 때도 멤버 이름이 동일한지
		List<String> result = queryFactory
			.select(member.username)
			.from(member)
			// .where(member.username.eq(
			// 	Expressions.stringTemplate("function('lower', {0})", member.username)))
			.where(member.username.eq(member.username.lower()))
			.fetch();

		for (String u : result) {
			System.out.println(u);
		}
	}
}
