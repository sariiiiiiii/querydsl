package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.hibernate.jpa.internal.PersistenceUnitUtilImpl;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.Test;
import study.querydsl.dto.MemberDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceUnitUtil;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
public class TestJPA {

    @Autowired
    EntityManager em;

    @Autowired
    TestMemberRepository testMemberRepository;

    @Autowired
    TestTeamRepository testTeamRepository;

    @Autowired
    TestMemberJpaRepository testMemberJpaRepository;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void init() {

        queryFactory = new JPAQueryFactory(em);

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
    public void testCase() {

        Member member = testMemberRepository.save(new Member("memberA"));

        Member findMember = testMemberRepository.findById(member.getId()).get();

        assertThat(member.getId()).isEqualTo(findMember.getId());
        assertThat(member.getUsername()).isEqualTo(findMember.getUsername());
        assertThat(member).isEqualTo(findMember);

        List<Member> result = testMemberRepository.findAll();

        assertThat(result.size()).isEqualTo(4);

        testMemberRepository.delete(member);

        long count = testMemberRepository.count();

        assertThat(count).isEqualTo(3);

    }

    @Test
    public void queryMethod() {

        List<Member> members = testMemberJpaRepository.findByPage(1, 3);

        for (Member member : members) {
            System.out.println("member = " + member);
        }

        long count = testMemberJpaRepository.totalCount();

        assertThat(count).isEqualTo(5);

    }

    @Test
    public void paging() throws InterruptedException {

        List<Member> result = testMemberRepository.findMemberCustom();

        for (Member member : result) {
            member.setUsername("asdfadfasdfasdfasdf");
        }

        Thread.sleep(1000);
        em.flush();
        em.clear();

        for (Member member : result) {
            System.out.println("member = " + member);
            System.out.println("getCreatedDate = " + member.getCreatedDate());
            System.out.println("getModifiedDate = " + member.getLastModifiedDate());
            System.out.println("member.getCreatedBy() = " + member.getCreatedBy());
            System.out.println("member.getLastModifiedBy() = " + member.getLastModifiedBy());
        }

        em.flush();

    }


}
