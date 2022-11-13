package study.querydsl;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
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

import java.util.List;

import static study.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
public class QuerydslIntermediateTest {

    @Autowired
    EntityManager em;
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
                .select(Projections.bean(MemberDto.class,
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
                .select(Projections.fields(MemberDto.class,
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
                .select(Projections.constructor(MemberDto.class,
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
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"), // 별칭(alias) 를 바꿔줌
//                        ExpressionUtils.as(member.username, "name"),

                        // name은 "name"로 설정을 해줬는데
                        // UserDto.age에 sub query조회한 값을 넣어주고 싶을 때
                        // ExpressionsUtils.as 후 두번째 파라미터로 alias 별칭 설정
                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
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
                .select(Projections.constructor(UserDto.class, // MemberDto.class => UserDto.class
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
}
