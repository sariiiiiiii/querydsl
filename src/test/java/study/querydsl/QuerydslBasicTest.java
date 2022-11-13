package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory; // JPAQueryFactory는 필드레벨로 가져가도 됨

    @BeforeEach
    public void before() {

        queryFactory = new JPAQueryFactory(em);
        
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

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
    @DisplayName("JPQL")
    public void startJPQL() {

        // member1를 찾아라.

        Member findMember = em.createQuery(
                        "select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "memberA")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("memberA");

    }

    @Test
    @DisplayName("Qtype 별칭(alias) 사용")
    public void startQuerydsl() {

        /**
         * JPAQueryFactory를 생성할 때 Entitymanager를 넣어줘야 Entitymanager를 통해서 entity를 관리
         */

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

    @Test
    @DisplayName("Qtype static import 사용")
    public void startQType() {

        queryFactory = new JPAQueryFactory(em);

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("memberA"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("memberA");

    }

    /**
     * 검색 조건 쿼리
     */
    @Test
    @DisplayName("where절 and 활용")
    public void search() {

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("memberA")
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("memberA");

    }

    /**
     * 검색 조건 쿼리
     * where절 and 생략 (, )
     */
    @Test
    @DisplayName("where절 and 생략(, )")
    public void searchAndParam() {

        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("memberA"),
                        (member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("memberA");

    }

    @Test
    @DisplayName("fetch()의 다양한 예제")
    public void resultFetchTest() {

        // 리스트 조회 : fetch()
        List<Member> members = queryFactory
                .selectFrom(member)
                .fetch();

        // 단건 조회 : fetchOne()
        Member member = queryFactory
                .selectFrom(QMember.member)
                .fetchOne();

        // 맨앞 정보 조회 : fetchFirst()
        Member fetchFirst = queryFactory
                .selectFrom(QMember.member)
//                .limit(1).fetchOne(); // 똑같음 fetchFirst()를 들어가보면
                .fetchFirst();

        // fetchResults() Deprecated(향후 미지원) => count query 따로 조회
        // fetchCount() Deprecated(향후 미지원) => count query 따로 조회
        Long totalCount = queryFactory
                // select(WildCard.count) // select count(*)
                .select(QMember.member.count()) // select count(member.id)
                .from(QMember.member)
                .fetchOne();

        System.out.println("totalCount = " + totalCount);


    }

}
