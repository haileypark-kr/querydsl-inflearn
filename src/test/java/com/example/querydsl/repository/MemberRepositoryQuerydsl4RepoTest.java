package com.example.querydsl.repository;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.example.querydsl.dto.MemberSearchCondition;
import com.example.querydsl.entity.Member;
import com.example.querydsl.entity.Team;

@SpringBootTest
class MemberRepositoryQuerydsl4RepoTest {

	@Autowired
	MemberTestRepository repository;

	@Autowired
	EntityManager em;

	@Test
	@Transactional
	public void searchPageByApplyPage() throws Exception {
		insertInitData();
		MemberSearchCondition condition = new MemberSearchCondition();
		PageRequest pageRequest = PageRequest.of(0, 4, Sort.Direction.DESC, "team.name");

		Page<Member> page = repository.searchPageByApplyPage(condition, pageRequest);

		System.out.println("size: " + page.getSize());
		System.out.println("total elements: " + page.getTotalElements());
		for (Member member : page.getContent()) {
			System.out.println(member);
		}
	}

	private void insertInitData() {
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
}
