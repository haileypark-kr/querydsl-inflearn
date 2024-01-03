package com.example.querydsl.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.querydsl.entity.Member;

/**
 * JpaRepository 인터페이스가 기본으로 제공하는 메소드는 그대로 사용하고,
 * 정적쿼리는 spring data jpa가 메소드명으로 쿼리 생성해줌.
 * 동적 쿼리는 어떻게 쓰느냐..
 */
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {

	// JPA가 메소드 이름으로 자동 쿼리 생성해줌
	// select m from Member m where m.username =: username
	List<Member> findByUsername(String username);
}
