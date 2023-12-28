package com.example.querydsl;

import javax.persistence.EntityManager;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

/**
 * QueryDSL 의 JPA Query Factory 등록하기 위한 설정.
 */
@Configuration
@RequiredArgsConstructor
public class JpaQueryFactoryConfig {

	private final EntityManager em; // 순수 JPA 사용하려면 entity manager 필요

	@Bean
	public JPAQueryFactory jpaQueryFactory(EntityManager em) {
		return new JPAQueryFactory(em);
	}
}
