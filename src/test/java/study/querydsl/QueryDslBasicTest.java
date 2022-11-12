package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
public class QueryDslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamAB");

        em.persist(teamA);
        em.persist(teamB);

        Member memberA = new Member("memberA", 10, teamA);
        Member memberB = new Member("memberB", 20, teamA);
        Member memberC = new Member("memberC", 30, teamB);
        Member memberD = new Member("memberD", 40, teamB);

        em.persist(memberA);
        em.persist(memberB);
        em.persist(memberC);
        em.persist(memberD);
    }

    @Test
    public void startJPQL() {

        // member1를 찾아라.

        Member findMember = em.createQuery(
                        "select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "memberA")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("memberA");

    }

    @Test
    public void startQuerydsl() {

        /**
         * JPAQueryFactory를 생성할 때 Entitymanager를 넣어줘야 Entitymanager를 통해서 entity를 관리
         */
        queryFactory = new JPAQueryFactory(em);

        /**
         * 어떤 qMember를 구분하는지 인자(별칭)로 넣어줘야됨
         */
        QMember m = new QMember("m");

        /**
         * querydsl의 parameter 바인딩은 preparestatement 방식의 파라미터 바인딩을 사용
         * DB입장에서 성능도 좋음
         */
        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("memberA"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("memberA");

    }

}
