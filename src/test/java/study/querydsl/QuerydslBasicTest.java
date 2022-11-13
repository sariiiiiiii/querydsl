package study.querydsl;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

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

        Member memberA = new Member("member1", 10, teamA);
        Member memberB = new Member("member2", 20, teamA);
        Member memberC = new Member("member3", 30, teamB);
        Member memberD = new Member("member4", 40, teamB);

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

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 오름차순(asc)
     * 3. 단, 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    @DisplayName("정렬")
    public void sort() {

        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> members = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        assertThat(members.get(0).getUsername()).isEqualTo("member5");
        assertThat(members.get(1).getUsername()).isEqualTo("member6");
        assertThat(members.get(2).getUsername()).isNull();
    }

    /**
     * 전체 조회수는 위 fetch()에 다양한 예제 count() 참고
     * fetchResult(), totalCount()는 deprecated
     */
    @Test
    @DisplayName("페이징")
    public void paging1() {

        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(0)
                .limit(3)
                .fetch();

        for (Member member1 : result) {
            System.out.println(member1.toString());
        }
        System.out.println("result.size() = " + result.size());

        assertThat(result.size()).isEqualTo(3);

    }

    @Test
    @DisplayName("집합")
    public void aggregation() {

        /**
         * Tuple : querydsl이 제공하는 객체
         * 반환값으로 받는 Tuple이란 여러개의 타입이 있을 떄 값을 꺼내올수 있음
         */

        List<Tuple> result = queryFactory
                .select(
                        member.count(), // 전체 회원 수
                        member.age.sum(), // 회원 나이 더하기
                        member.age.avg(), // 회원 나이 평균
                        member.age.max(), // 회원 최대 나이
                        member.age.min()) // 회원 최소 나이
                .from(member)
                .fetch();

        /**
         * Tuple에서 값을 꺼내올 떄는 querydsl에서 쓴 그대로 가져와서 쓰면 됨
         */
        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);

    }

    /**
     * 팀의 이름과 각팀의 평균 연령을 구해라
     */
    @Test
    @DisplayName("팀의 이름과 각팀의 평균 연령을 구해라")
    public void groupBy() throws Exception {

        // given
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team) // member에 있는 team과 team을 join
                .groupBy(team.name) // 팀의 이름으로 grouping
                .fetch();

        // when
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        // then
        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * 팀A에 소속된 모든 회원을 찾아라
     */
    @Test
    @DisplayName("기본 조인")
    public void join() {

        /**
         * 조인의 기본 문법은 첫 번째 파라미터에 조인 대상을 지정하고, 두 번째 파라미터에 별칭(alias)으로 사용할 Q 타입을 지정하면 됨
         */

        List<Member> members = queryFactory
                .selectFrom(member)
                .join(member.team, team) // 두 번째 파라미터는 QTeam.team을 말하는 것
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(members)
                .extracting("username") // username은
                .containsExactly("member1", "member2"); // member1과 member2가 있음

    }

    /**
     * 세타 조인
     * 회원의 이름이 팀 이름과 같은 회원 조회
     * 외부 조인 불가능 => 다음에 설명할 조인 on을 사용하면 외부 조인 가능
     */
    @Test
    @DisplayName("세타 조인")
    @Commit
    public void theta_join() {

        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        /**
         * 모든 회원과 모든 팀을 다 조인해서 where절 (막 조인)
         */
        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 조인 on절
     * 조인 대상 필터링
     * 연관관계 없는 엔티티 외부 조인
     * 회원과 팀을 조회(조인)하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL : select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    @DisplayName("조인 on절")
    public void join_on_filtering() {

        List<Tuple> result = queryFactory // select가 여러가지 타입으로 나와서 Tuple객체로 반환
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
//                .where(team.name.eq("teamA")) // 내부조인일 떄는 where절로 해도 똑같은 결과
                .fetch();

        /**
         * member1과 member2는 teamA 소속이니까 team정보도 가져오고
         * member3과 member4는 teamA 소식이 아니기 떄문에 team정보는 null로 가져옴 left outer join
         * 내부 조인(inner join)일때는 on절을 사용하지 않고 where절을 사용해도 똑같은 결과가 나옴
         * 외부 조인일 경우에는 null값도 가져와야 하기 때문에 where절로는 해결할 수 없어서 on절로 해결해야 함
         * 조인 대상 필터링을 할 때, 내부조인이면 익숙한 where절로 해결하고, 정말 외부조인이 필요한 경우에는 on절로 해결
         */
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 연관관계가 없는 엔티티 외부 조인
     * 회원의 이름이 팀 이름과 같은 대상 외부 조인
     */
    @Test
    @DisplayName("연관관계가 없는 엔티티 외부조인")
    public void join_on_no_relation() {

        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        /**
         * 연관관계에서 on절 했을 떄는 on절에 id를 매칭하는데
         * 연관관계가 아닌 상황에서는 단순히 name으로 on절 필터링
         */

        // 원래 연관관계가 있을 떄는 from절에 member와 leftJoin(member.team, team) 조인을 했는데 막 조인할 때는 그냥 QType 바로 지정
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team)
                .on(team.name.eq(member.username))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    @DisplayName("조인 - 페치조인(미적용)")
    public void fetchJoinNo() {

        /**
         * 페치 조인은 SQL에서 제공하는 기능은 아니다. SQL조인을 활용해서 연관된 엔티티를
         * SQL 한번에 조회하는 기능이다. 주로 성능 최적화에 사용하는 방법
         */

        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        // 이미 로딩된 entity인지 초기화가 안된 엔티티인지 가르쳐주는 메서드
        boolean loaded = emf.getPersistenceUnitUtil()
                .isLoaded(findMember.getTeam());

        assertThat(loaded).as("페치 조인 미적용").isFalse();

    }

    @Test
    @DisplayName("조인 - 페치조인(적용)")
    public void fetchJoin() {

        /**
         * 페치 조인은 SQL에서 제공하는 기능은 아니다. SQL조인을 활용해서 연관된 엔티티를
         * SQL 한번에 조회하는 기능이다. 주로 성능 최적화에 사용하는 방법
         */

        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin() // 기본 join이랑 똑같은 문법인데 뒤에 .fetchJoin() 추가
                .where(member.username.eq("member1"))
                .fetchOne();

        System.out.println("findMember = " + findMember);
        System.out.println("===================================================");
        System.out.println("findMember = " + findMember.getTeam().getName());

        // 이미 로딩된 entity인지 초기화가 안된 엔티티인지 가르쳐주는 메서드
        boolean loaded = emf.getPersistenceUnitUtil()
                .isLoaded(findMember.getTeam());

        assertThat(loaded).as("페치 조인 적용").isTrue();
    }

    /**
     * from절의 서브쿼리 한계
     * JPA JPQL 서브쿼리의 한계점으로 from절의 서브쿼리(인라인 뷰)는 지원하지 않는다
     * 당연히 querydsl도 지원하지 않는다. 하이버네이트 구현체를 사용하면 select절의 서브쿼리는 지원한다
     * Querydsl도 하이버네이트 구현체를 사용하면 select절의 서브쿼리를 지원한다
     */

    /**
     * from절의 서브쿼리 해결방안
     * 서브쿼리를 join으로 변경한다 (가능한 상황이 있고, 불가능한 상황이 있음)
     * 애플리케이션에서 쿼리를 2번 분리해서 실행
     * nativeSQL을 사용
     */

    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    @DisplayName("서브 쿼리 eq(equals)")
    public void subQuery() {

        /**
         * 서브쿼리에 Member랑 조회대상 member랑 겹치면 안되기 때문에
         * QMember를 생성해서 사용 alias가 중복되면 안되기 때문
         */

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq( // member의 나이가
                        // JPAExpressions 서브쿼리 시작
                        select(memberSub.age.max()) // member의 나이중 가장 많은 나이 다른 QType
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result)
                .extracting("age")
                .containsExactly(40);

    }

    /**
     * 나이가 평균이상인 회원 조회
     */
    @Test
    @DisplayName("서브 쿼리 goe (greater or equals)")
    public void subQueryGoe() {

        /**
         * 서브쿼리에 Member랑 조회대상 member랑 겹치면 안되기 때문에
         * QMember를 생성해서 사용 alias가 중복되면 안되기 때문
         */

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe( // member의 나이가 크거나 같은 조건
                        // JPAExpressions 서브쿼리 시작
                        select(memberSub.age.avg()) // member의 평균 나이
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result)
                .extracting("age")
                .containsExactly(30, 40);

    }

    /**
     * 나이가 평균이상인 회원 조회
     */
    @Test
    @DisplayName("서브 쿼리 in")
    public void subQueryIn() {

        /**
         * 서브쿼리에 Member랑 조회대상 member랑 겹치면 안되기 때문에
         * QMember를 생성해서 사용 alias가 중복되면 안되기 때문
         */

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in( // member의 나이는 where절 시작
                        //      JPAExpressions. static import
                                select(memberSub.age) // member 나이 출력
                                        .from(memberSub) // member 중에
                                        .where(memberSub.age.gt(10)) // member의 나이가 10살 초과인 나이들 where절
                        )
                )
                .fetch();

        assertThat(result)
                .extracting("age")
                .containsExactly(20, 30, 40);

    }

    /**
     * 나이가 평균이상인 회원 조회
     */
    @Test
    @DisplayName("select절 서브쿼리")
    public void selectSubQuery() {

        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
//                        JPAExpressions. static import
                        select(memberSub.age.avg())
                                .from(memberSub)
                )
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * Case 문
     * select, where(조건절)에서 사용 가능
     */
    @Test
    @DisplayName("Case문")
    public void basicCase() {

        List<Tuple> result = queryFactory
                .select(member.age
                                .when(10).then("열 살")
                                .when(20).then("스무살")
                                .otherwise("기타"),
                        member.username)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    @DisplayName("복잡한 조건 Case")
    public void complexCase() {

        List<Tuple> result = queryFactory
                .select(new CaseBuilder()
                                .when(member.age.between(0, 20)).then("0 ~ 20살")
                                .when(member.age.between(21, 30)).then("21 ~ 30살")
                                .otherwise("기타"),
                        member.username)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    @DisplayName("상수 필요시")
    public void constant() {

        /**
         * JPQL에서는 query가 나가지 않고 데이터를 받을때만 상수를 받음
         */

        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    @DisplayName("문자 더하기")
    public void concat() {

        /**
         * 예제) {username}_{age}
         * 나이(member.age)는 타입언어가 아니다
         * stringValue()라는 것을 이용해 형변환 해서 사용
         * ENUM타입같은 경우는 값이 안나와서 stringValue()를 사용
         * 생각보다 stringValue()를 사용할 일이 많음
         */

        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue())) // query 나갈 떄 casting되서 나감 = stringValue()
                .from(member)
                .fetch();

        for (String username : result) {
            System.out.println("username = " + username);
        }
    }

}
