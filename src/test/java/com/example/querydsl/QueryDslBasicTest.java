package com.example.querydsl;

import static com.example.querydsl.entity.QMember.*;
import static com.example.querydsl.entity.QTeam.*;
import static org.assertj.core.api.Assertions.*;

import java.util.List;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.querydsl.entity.Member;
import com.example.querydsl.entity.Team;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
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
}
