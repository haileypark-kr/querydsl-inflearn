package com.example.querydsl.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.querydsl.dto.MemberSearchCondition;
import com.example.querydsl.dto.MemberTeamDto;
import com.example.querydsl.entity.Member;
import com.example.querydsl.entity.Team;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {
	@Autowired
	EntityManager em;

	@Autowired
	MemberJpaRepository memberJpaRepository;

	@Test
	@Transactional
	public void basicTest() throws Exception {
		// given
		Member member1 = Member.builder().username("member1").age(10).build();
		memberJpaRepository.save(member1);

		// when
		Member findMember = memberJpaRepository.findById(member1.getId()).get();
		assertThat(member1).isEqualTo(findMember);

		List<Member> result = memberJpaRepository.findAll();
		assertThat(result).contains(member1);

		List<Member> result2 = memberJpaRepository.findByUsername("member1");
		assertThat(result).containsExactly(member1);
	}

	@Test
	@Transactional
	public void basicQueryDSLTest() throws Exception {
		// given
		Member member1 = Member.builder().username("member1").age(10).build();
		memberJpaRepository.save(member1);

		// when
		Member findMember = memberJpaRepository.findById(member1.getId()).get();
		assertThat(member1).isEqualTo(findMember);

		List<Member> result = memberJpaRepository.findAll_QueryDSL();
		assertThat(result).contains(member1);

		List<Member> result2 = memberJpaRepository.findByUsername_QueryDSL("member1");
		assertThat(result).containsExactly(member1);
	}

	@Test
	@Transactional
	public void search() throws Exception {

		insertInitData();

		MemberSearchCondition condition = new MemberSearchCondition();
		condition.setAgeGoe(35);
		condition.setAgeLoe(40);
		condition.setTeamName("teamB");

		// List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(condition);
		List<MemberTeamDto> result = memberJpaRepository.search(condition);

		assertThat(result).extracting("username").containsExactly("member4");
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
