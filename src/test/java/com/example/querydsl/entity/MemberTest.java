package com.example.querydsl.entity;

import java.util.List;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Transactional
@SpringBootTest
class MemberTest {

	@Autowired
	EntityManager em;

	@Test
	public void testEntity() throws Exception {
		// given
		Team teamA = Team.builder().name("team A").build();
		Team teamB = Team.builder().name("team B").build();
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

		// 초기화
		em.flush(); // => 영속성 컨텍스트에 있는 내용은 commit
		em.clear(); // => 영속성 컨텍스트에 있는 캐시 날림

		// when

		List<Member> members = em.createQuery("select m from Member m", Member.class)
			.getResultList();

		// then
		for (Member member : members) {
			System.out.println("member = " + member);
			System.out.println(" - member.team = " + member.getTeam());
		}
	}
}
