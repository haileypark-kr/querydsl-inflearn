package com.example.querydsl;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.querydsl.entity.Member;
import com.example.querydsl.entity.Team;

import lombok.RequiredArgsConstructor;

@Profile("local")
@Component
@RequiredArgsConstructor
public class InitData {
	private final InitMemberService initMemberService;

	@PostConstruct
	// @Transactional // Spring lifecycle에서 PostConstruct와 Transactional는 같이 못쓴다.
	public void init() {
		initMemberService.init();
	}

	@Component
	static class InitMemberService {

		@PersistenceContext
		private EntityManager em;

		@Transactional
		public void init() {
			Team teamA = Team.builder().name("teamA").build();
			Team teamB = Team.builder().name("teamB").build();
			em.persist(teamA);
			em.persist(teamB);

			for (int i = 0; i < 100; i++) {
				Team selctedTeam = i % 2 == 0 ? teamA : teamB;
				em.persist(Member.builder().username("member" + i).age(i).team(selctedTeam).build());
			}
		}
	}
}
