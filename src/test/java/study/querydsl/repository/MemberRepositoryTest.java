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
         * QuerydslPredicateExecutor<Type>??? ??????????????? spring data JPA??? ???????????? ?????????????????? querydsl predicate querydsl??? ????????? ?????? ??? ??????
         *   * ?????? X (????????? ????????? ??????????????? left join??? ?????????)
         *   * ?????????????????? Querydsl??? ???????????? ??????. ????????? ???????????? Querydsl????????? ?????? ????????? ???????????? ??????.
         *   * ????????? ?????????????????? ?????????????????? ????????? ????????????.
         *     * QuerydslPredicateExecutor??? pageable, Sort??? ?????? ???????????? ?????? ????????????
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