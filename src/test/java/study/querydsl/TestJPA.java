package study.querydsl;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.Test;
import study.querydsl.entity.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

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

    @Autowired
    EntityManagerFactory emf;

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

    @Test
    void contextLoads() {

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);
        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        em.flush();
        em.clear();

        List<Tuple> result = queryFactory
                .select(member.age.count(),
                        member.age.min(),
                        member.age.max(),
                        member.age.avg(),
                        member.age.sum())
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);

        assertThat(tuple.get(member.age.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);

    }

    @Test
    public void group() {

        List<Tuple> result = queryFactory
                .select(team.name,
                        member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .having(member.age.avg().goe(40))
                .fetch();

        Tuple teamA = result.get(0);

        System.out.println("teamA.toString() = " + teamA.toString());

    }

    @Test
    public void join() {

        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("memberA", "memberB", "memberC");

    }

    @Test
    public void theta_join() {

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

    }

    @Test
    public void onJoin() {

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void fetchJoin_v1() {

        em.flush();
        em.clear();

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("memberA"))
                .fetchOne();

        System.out.println("findMember.getTeam().getClass() = " + findMember.getTeam().getClass());

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isFalse();

    }

    @Test
    public void fetchJoin_v2() {

        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("memberA"))
                .fetchOne();

        System.out.println("findMember.getTeam().getClass() = " + findMember.getTeam().getClass());

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 적용").isTrue();

    }

}
