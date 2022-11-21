package study.querydsl.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberRepository memberRepository;

    @Test
    public void basicTest() {

        Member member = new Member("member1", 10);
        memberRepository.save(member);

        Member findMember = memberRepository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

        List<Member> result1 = memberRepository.findAll();
        assertThat(result1).containsExactly(member);

        List<Member> result2 = memberRepository.findByUsername("member1");
        assertThat(result2).containsExactly(member);
    }

    @Test
    @DisplayName("querydsl paging simple")
    public void searchTest2() {

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member memberA = new Member("memberA", 10, teamA);
        Member memberB = new Member("memberB", 20, teamA);
        Member memberC = new Member("memberC", 30, teamA);
        Member memberD = new Member("memberD", 40, teamB);
        Member memberE = new Member("memberE", 50, teamB);

        em.persist(memberA);
        em.persist(memberB);
        em.persist(memberC);
        em.persist(memberD);
        em.persist(memberE);

        MemberSearchCondition condition = new MemberSearchCondition();

        PageRequest request = PageRequest.of(0, 3);

        Page<MemberTeamDto> result = memberRepository.searchPageSimple(condition, request);

        assertThat(result.getSize()).isEqualTo(3);
        assertThat(result.getContent())
                .extracting("username")
                .containsExactly("memberA", "memberB", "memberC");

    }

    @Test
    public void searchPageSimple() {

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member memberA = new Member("memberA", 10, teamA);
        Member memberB = new Member("memberB", 20, teamA);
        Member memberC = new Member("memberC", 30, teamA);
        Member memberD = new Member("memberD", 40, teamB);
        Member memberE = new Member("memberE", 50, teamB);

        em.persist(memberA);
        em.persist(memberB);
        em.persist(memberC);
        em.persist(memberD);
        em.persist(memberE);
    }

    @Test
    @DisplayName("querydslPredicateExecutorTest")
    public void querydslPredicateExecutorTest() {

        /**
         * QuerydslPredicateExecutor<Type>을 상속받으면 spring data JPA가 제공하는 인터페이스에 querydsl predicate querydsl의 조건을 넣을 수 있음
         *   * 조인 X (묵시적 조인은 가능하지만 left join이 불가능)
         *   * 클라이언트가 Querydsl에 의존해야 한다. 서비스 클래스가 Querydsl이라는 구현 기술에 의존해야 한다.
         *   * 복잡한 실무환경에서 사용하기에는 한계가 명확하다.
         *     * QuerydslPredicateExecutor는 pageable, Sort를 모두 지원하고 정상 동작한다
         */

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member memberA = new Member("memberA", 10, teamA);
        Member memberB = new Member("memberB", 20, teamA);
        Member memberC = new Member("memberC", 30, teamA);
        Member memberD = new Member("memberD", 40, teamB);
        Member memberE = new Member("memberE", 50, teamB);

        em.persist(memberA);
        em.persist(memberB);
        em.persist(memberC);
        em.persist(memberD);
        em.persist(memberE);

        em.flush();
        em.clear();

        Iterable<Member> result = memberRepository.findAll(member.age.between(10, 30).and(member.username.eq("memberA")));

        for (Member findMember : result) {
            System.out.println("member1 = " + findMember);
        }
    }

}