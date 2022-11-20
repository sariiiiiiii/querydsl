package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.*;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import java.util.List;

import static com.querydsl.core.types.Projections.*;
import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QuerydslIntermediateTest {

    @Autowired
    EntityManager em;

    @Autowired
    EntityManagerFactory emf;

    JPAQueryFactory queryFactory;


    /**
     * 프로젝션과 결과 반환 - 기본
     * 프로젝션 : select 대상 지정
     */

    @Test
    @DisplayName("프로젝션 대상이 하나")
    public void simpleProjection() {

        /**
         * 프로젝션 대상이 하나
         * ex) String, Integer, Long, Member, Team (단일 대상)
         */

        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String username : result) {
            System.out.println("username = " + username);
        }
    }

    @Test
    @DisplayName("프로젝션 대상이 여러개")
    public void tupleProjection() {

        /**
         * Tuple 객체에서 값을 꺼내고 싶으면
         * Tuple.get(querydsl 문법)으로 Qtype객체 지정 문법으로 꺼내주어야 함
         *
         * Tuple도 repository 계층에서 쓰는것으로 하고,
         * 바깥으로 던질때는 DTO로 반환하여 던져주는 것을 권장 !!
         */

        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    @Test
    @DisplayName("순수 JPA로 DTO 반환")
    public void findDto() {
        List<MemberDto> result = em.createQuery(
                        "select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * Querydsl 빈 생성 (Bean population)
     * 결과를 DTO 반환할 때 사용
     * 다음 3가지 방법 지원
     * 프로퍼티 접근 - setter
     * 필드 직접 접근
     * 생성자 사용
     *
     * DTO class에 기본생성자는 만들어줘야 됨
     */

    /**
     * 프로퍼티 접근 - setter
     * Projections.bean() 방식은 setter 기반으로 동작하게 됨
     * 그러기 때문에 MemberDtoBean 객체의 setter 메서드를 열어야 함
     */
    @Test
    @DisplayName("프로퍼티 접근 - setter")
    public void findDtoBySetter() {

        List<MemberDto> result = queryFactory
                .select(bean(MemberDto.class,
                        member.username,
                        member.age)) // setter로 데이터 injection (type 지정 필수)
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

    }

    /**
     * 필드 직접 접근
     * Projections.fields() 바로 field에 데이터를 꽂아버림
     */
    @Test
    @DisplayName("필드 직접 접근")
    public void findDtoByField() {

        List<MemberDto> result = queryFactory
                .select(fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * 생성자 접근 방법
     * Projections.constructor
     * Dto class에 생성한 생성자의 타입과 딱딱 맞아줘야 값이 들어감
     */
    @Test
    @DisplayName("생성자 접근")
    public void findDtoByConstructor() {

        List<MemberDto> result = queryFactory
                .select(constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * 별칭이 다를 때 field 접근 방법
     * Member를 조회하는데 UserDto로 값을 넣고 싶을 때
     * setter는 property값이 맞아야되고
     * field는 field명이 같아야 됨
     * 조회할 때 오류는 나지 않지만 매칭이 안되서 매칭안된 값이 null로 조회
     */
    @Test
    @DisplayName("별칭이 다를 때")
    public void findUserDto() {

        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = queryFactory
                .select(fields(UserDto.class,
                        member.username.as("name"), // 별칭(alias) 를 바꿔줌
//                        ExpressionUtils.as(member.username, "name"),

                        // name은 "name"로 설정을 해줬는데
                        // UserDto.age에 sub query조회한 값을 넣어주고 싶을 때
                        // ExpressionsUtils.as 후 두번째 파라미터로 alias 별칭 설정
                        ExpressionUtils.as(select(memberSub.age.max())
                                .from(memberSub), "age")

                ))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    /**
     * 별칭이 다를 때 생성자 접근 방법
     * 값을 넣어줄 DTO class에 기본 생성자와 오버로딩 생성자를 만들어주고 type만 바꾸면 끝
     */
    @Test
    @DisplayName("별칭이 다를 때")
    public void findUserDtoConstructor() {

        List<UserDto> result = queryFactory
                .select(constructor(UserDto.class, // MemberDto.class => UserDto.class
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    /**
     * @QueryProjection을 활용한 DTO로 바로 데이터 넣기
     */
    @Test
    @DisplayName("DTO class QueryProjection 활용")
    public void findDtoByQueryProjection() {

        /**
         * 생성된 QMemberDto의 생성자에는 type도 딱 맞춰 생성되어있기 때문에
         * 컴파일 시점에 오류도 잡아줄 수 있음
         *
         * 예) 생성자에 값을 잘못 넣어줬을 때
         * Projections.constructor = 컴파일 시점에 오류를 잡아주지 못하고 메서드 실행시 런타임 오류
         * @QueryProjection = 컴파일 시점에 오류를 잡아줌
         *
         * 한가지의 고민거리 ...
         *  * QType을 생성해야 되는 것
         *  * 아키텍쳐적인 고민 : Dto는 querydsl에 대한 라이브러리 의존성이 없었는데 @QueryProjection을 사용하는 순간 querydsl에 대한 의존성을 가지게 됨
         *    * DTO는 service, controller 계층에서도 사용하는데 dto의 순수성을 잃게 됨
         */

        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

    }

    @BeforeEach
    public void init() {
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
    @DisplayName("기본 조인")
    void join() throws Exception {

        //given
        em.flush();
        em.clear();

        //when
        List<Member> result = queryFactory
                .select(member)
                .from(member)
                .join(member.team, team)
                .fetch();

        //then
        assertThat(result.size()).isEqualTo(4);
        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2", "member3", "member4");

    }

    @Test
    @DisplayName("세타 조인 (연관관계 관련 없음)")
    void theta_join() throws Exception {

        //given
        em.flush();
        em.clear();

        //when
        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        //then
        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");

    }

    @Test
    @DisplayName("조인 대상 필터링")
    void join_on_filtering() throws Exception {

        /**
         * outer join을 할 경우에는 on절이 유리하지만
         * inner join의 경우에는 where로 하는것이 더 좋음
         */

        //given
        em.flush();
        em.clear();

        //when
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        //then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

    }

    @Test
    @DisplayName("연관관계 없는 엔티티 외부 조인")
    void join_on_no_relation() throws Exception {

        //given
        em.flush();
        em.clear();

        //when
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        //then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    @DisplayName("페치 조인 미적용")
    void no_fetchJoin() throws Exception {

        //given
        em.flush();
        em.clear();

        //when
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        //then
        Boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isFalse();

    }

    @Test
    @DisplayName("페치 조인")
    void fetchJoin() throws Exception {

        //given
        em.flush();
        em.clear();

        //when
        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        System.out.println("findMember = " + findMember);

        //then
        Boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 적용").isTrue();

    }

    @Test
    @DisplayName("서브 쿼리 eq 사용")
    void subQuery() throws Exception {

        QMember subMember = new QMember("m");

        //given
        em.flush();
        em.clear();

        //when
        List<Member> result = queryFactory
                .select(member)
                .from(member)
                .where(member.age.eq(
                        select(subMember.age.max())
                                .from(subMember)
                ))
                .fetch();

        //then
        assertThat(result)
                .extracting("age")
                .containsExactly(40);

    }

    @Test
    @DisplayName("서브 쿼리 goe 사용")
    void subQueryGoe() throws Exception {

        //given
        em.flush();
        em.clear();

        QMember subMember = new QMember("m");

        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(subMember.age.avg())
                                .from(subMember)
                ))
                .fetch();

        //then
        assertThat(result)
                .extracting("age")
                .containsExactly(30, 40);

    }

    @Test
    @DisplayName("서브쿼리 여러건 처리 in 사용")
    void subQueryIn() throws Exception {

        //given
        em.flush();
        em.clear();
        QMember subMember = new QMember("m");

        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(subMember.age)
                                .from(subMember)
                                .where(subMember.age.gt(10))
                ))
                .fetch();

        //then
        assertThat(result)
                .extracting("age")
                .containsExactly(20, 30, 40);

    }

    @Test
    @DisplayName("select 절에 subQuery")
    void selectSubQuery() throws Exception {

        //given
        em.flush();
        em.clear();
        QMember subMember = new QMember("m");

        //when
        List<Tuple> result = queryFactory
                .select(member.username,
                        select(subMember.age.avg())
                                .from(subMember)
                )
                .from(member)
                .fetch();

        //then
        for (Tuple tuple : result) {
            System.out.println("username = " + tuple.get(member.username));
            System.out.println("age = " + tuple.get(select(subMember.age.avg())
                    .from(subMember)));
        }
    }

    @Test
    @DisplayName("case 문 사용")
    void selectCaseQuerydsl() throws Exception {

        //given
        em.flush();
        em.clear();

        //when
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열 살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        //then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    @DisplayName("복잡한 조건")
    void betweenQuery() throws Exception {

        //given
        em.flush();
        em.clear();

        //when
        List<Tuple> result = queryFactory
                .select(member.username, new CaseBuilder()
                        .when(member.age.between(10, 20)).then("열살에서 스무살")
                        .when(member.age.between(20, 30)).then("스무살에서 서른살")
                        .when(member.age.between(30, 40)).then("서른살에서 마흔살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        //then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
            System.out.println("age = " + tuple.get(1, String.class));
        }
    }

    @Test
    @DisplayName("orderBy case문 함께 사용")
    void numberExpression() throws Exception {

        //given
        em.flush();
        em.clear();

        //when
        // 복잡한 case문 변수 사용
        NumberExpression<Integer> rankPath = new CaseBuilder()
                .when(member.age.between(0, 20)).then(2)
                .when(member.age.between(21, 30)).then(1)
                .otherwise(3);

        List<Tuple> result = queryFactory
                .select(member.username, member.age, rankPath)
                .from(member)
                .orderBy(rankPath.desc())
                .fetch();

        //then
        for (Tuple tuple : result) {
            System.out.println("tuple.get(member.username) = " + tuple.get(member.username));
            System.out.println("tuple.get(member.age) = " + tuple.get(member.age));
            System.out.println("tuple.get(rankPath) = " + tuple.get(rankPath));
        }
    }

    @Test
    @DisplayName("")
    void constant() throws Exception {

        //given
        em.flush();
        em.clear();

        //when
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        //then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    @DisplayName("concat 사용")
    void concat() throws Exception {

        //given
        em.flush();
        em.clear();

        //when
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .fetch();

        //then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    @DisplayName("순수 JPA에서 DTO코드 조회(생성자 방식)")
    void dto() throws Exception {

        //given
        em.flush();
        em.clear();

        //when
        List<MemberDto> result = em.createQuery(
                        "select new study.querydsl.dto.MemberDto(m) from Member m", MemberDto.class)
                .getResultList();

        //then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    @DisplayName("프로퍼티 접근")
    void projectionSetter() throws Exception {

        //given
        em.flush();
        em.clear();

        //when
        List<MemberDto> result = queryFactory
                .select(bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        //then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    @DisplayName("필드 직접 접근")
    void filedProjection() throws Exception {

        //given
        em.flush();
        em.clear();

        //when
        List<MemberDto> result = queryFactory
                .select(fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        //then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    @DisplayName("생성자 사용")
    void constructorProjection() throws Exception {

        //given
        em.flush();
        em.clear();

        //when
        List<MemberDto> result = queryFactory
                .select(constructor(MemberDto.class,
//                        member, // entity 직접 주입
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        //then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    @DisplayName("@QueryProjection 활용")
    void annotationConstructorProjection() throws Exception {

        //given
        em.flush();
        em.clear();

        //when
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member))
                .from(member)
                .fetch();

        //then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    @DisplayName("distinct 중복제거")
    void querydslDistinct() throws Exception {

        //given
        em.flush();
        em.clear();

        //when
        List<String> result = queryFactory
                .select(member.username).distinct()
                .from(member)
                .fetch();

        //then
        for (String s : result) {
            System.out.println("username = " + s);
        }
    }

    @Test
    @DisplayName("동적 builder")
    void 동적쿼리_booleanBuilder() throws Exception {

        //given
        em.flush();
        em.clear();
        String username = "member1";
        Integer age = 10;

        //when
        BooleanBuilder booleanBuilder = new BooleanBuilder();

        if (username != null) {
            booleanBuilder.and(member.username.eq(username));
        }
        if (age != null) {
            booleanBuilder.and(member.age.eq(age));
        }

        List<Member> result = queryFactory
                .select(member)
                .from(member)
                .where(booleanBuilder)
                .fetch();

        //then
        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    @Test
    @DisplayName("동적 쿼리 - BooleanBuilder 사용")
    void dynamicQuery_BooleanBuilder() throws Exception {

        //given
        String usernameParam = "member1";
        Integer ageParam = 10;

        //when
        List<Member> result = searchMember1(usernameParam, ageParam);

        //then
        assertThat(result.size()).isEqualTo(1);

    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        /**
         * 동적 쿼리 생성
         * BooleanBuilder의 초기값을 넣어줄 수 있음 new BooleanBuilder(member.username.eq(usernameCond));
         * 하지만, usernameCond가 파라미터로 들어올 때 null이 들어오면 안되게끔 방어코드를 타고 온 상태이여야 한다 (null값이 들어오면 안되니까)
         * 그러면 초기값을 넣어줄 수 있음
         */

        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond)); // username where절 생성
        }
        if (ageCond != null) {
            builder.and(member.age.eq(ageCond)); // age where절 생성
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    @Test
    @DisplayName("동적 쿼리 - where 다중 파라미터 사용")
    void dynamicQuery_WhereParam() throws Exception {

        /**
         * where절 다중 파라미터의 장점
         * where 조건에 null값은 실제 query에 안나감
         * 쿼리 자체의 코드 가독성이 좋아짐
         * 메서드로 따로 빼기 때문에 재사용성 굉장히 좋음
         */

        //given
        String usernameParam = "member1";
        Integer ageParam = 10;

        //when
        List<Member> result = searchMember2(usernameParam, ageParam);

        //then
        assertThat(result.size()).isEqualTo(1);

    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))
//                .where(allEq(usernameCond, ageCond)) // where절 조립한 메서드 사용
                .fetch();
    }

    // username where절 동적
    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    // age where절 동적
    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    // 동적 쿼리 나눠 놓은 메서드 조립
    private Predicate allEq(String usernameCond, Integer ageCond) {

        /**
         * 조립한 메서드 방석일 때도 null 처리는 따로 해줘야 함
         * null 체크만 조심 !
         */

        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    /**
     * 벌크 연산
     */
    @Test
    @DisplayName("벌크 연산")
    void bulkUpdate() throws Exception {
        // member1, member2 -> 비회원

        long result = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        // 영속성 컨텍스트 초기화 (벌크연산 단점 보완)
        em.flush();
        em.clear();

        assertThat(result).isEqualTo(2);
    }

    @Test
    @DisplayName("벌크 더하기")
    void bulkAdd() throws Exception {

        /**
         * QType의 add는 있고 minus는 없어서 빼고 싶으면 add(-1)
         * 곱하기는 Multiply(?)
         */

        long result = queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();

        // 영속성 컨텍스트 초기화 (벌크연산 단점 보완)
        em.flush();
        em.clear();

        assertThat(result).isEqualTo(4);
    }

    @Test
    @DisplayName("벌크연산 삭제")
    void bulkDelete() throws Exception {

        long result = queryFactory
                .delete(member)
                .where(member.age.gt(30))
                .execute();

        assertThat(result).isEqualTo(1);
    }

    /**
     * SQL Function 호출
     * 조회를 할건데 member.username에서 member라는 단어를 M이라는 단어로 replace
     * String이면 StringTemplate
     * 참고로 function은 DBDilect에 등록이 되어 있어야 한다
     * 만약 나만의 function을 만들고 싶으면 H2Direct를 상속을 받아서 설정파일로 수정해줘야됨 (JPA 기본편 참고)
     */
    @Test
    public void sqlFunction() {

        List<String> result = queryFactory
                .select(
                        Expressions.stringTemplate("function('replace', {0}, {1}, {2})", member.username, "member", "M")
                )
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * 이런 간단한 함수들은 (안시표준) querydsl이 어느정도 내장을 하고 있다
     * sqlFunction3 테스트 케이스를 참고
     */
    @Test
    public void sqlFuction2() {

        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .where(member.username.eq(
                        Expressions.stringTemplate("function('lower', {0})", member.username)))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void sqlFunction3() {

        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .where(member.username.eq(member.username.lower()))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }


}