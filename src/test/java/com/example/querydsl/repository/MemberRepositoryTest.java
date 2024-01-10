package com.example.querydsl.repository;

import static com.example.querydsl.entity.QMember.*;
import static org.assertj.core.api.Assertions.*;

import java.util.List;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import com.example.querydsl.dto.MemberSearchCondition;
import com.example.querydsl.dto.MemberTeamDto;
import com.example.querydsl.entity.Member;
import com.example.querydsl.entity.Team;

@SpringBootTest
@Transactional
class MemberRepositoryTest {
	@Autowired
	EntityManager em;

	@Autowired
	MemberRepository memberRepository;

	@Test
	@Transactional
	public void basicTest() throws Exception {
		// given
		Member member1 = Member.builder().username("member1").age(10).build();
		memberRepository.save(member1);

		// when
		Member findMember = memberRepository.findById(member1.getId()).get();
		assertThat(member1).isEqualTo(findMember);

		List<Member> result = memberRepository.findAll();
		assertThat(result).contains(member1);

		List<Member> result2 = memberRepository.findByUsername("member1");
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

		List<MemberTeamDto> result = memberRepository.search(condition);

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

	@Test
	@Transactional
	public void searchPageSimple() throws Exception {

		insertInitData();

		MemberSearchCondition condition = new MemberSearchCondition();

		PageRequest pageRequest = PageRequest.of(0, 3);
		Page<MemberTeamDto> result = memberRepository.searchPageSimple(condition, pageRequest);

		assertThat(result.getSize()).isEqualTo(3);
		assertThat(result.getContent()).extracting("username").containsExactly("member1", "member2", "member3");

	}

	@Test
	@Transactional
	public void querydslPredicateExecutorTest() throws Exception {
		insertInitData();

		Iterable<Member> results = memberRepository.findAll(
			member.age.between(10, 30).and(member.username.eq("member1")));

		for (Member member : results) {
			System.out.println(member);
		}
	}

	@Test
	@Transactional
	public void searchPageSimple_QuerydslRepositorySupport() throws Exception {
		insertInitData();

		MemberSearchCondition condition = new MemberSearchCondition();
		PageRequest pageRequest = PageRequest.of(0, 2, Sort.Direction.DESC, "age");

		Page<MemberTeamDto> page = memberRepository.searchPageSimple_QuerydslRepositorySupport(condition,
			pageRequest);
		System.out.println("size: " + page.getSize());
		System.out.println("total elements: " + page.getTotalElements());
		for (MemberTeamDto memberTeamDto : page.getContent()) {
			System.out.println(memberTeamDto);
		}
	}
}
