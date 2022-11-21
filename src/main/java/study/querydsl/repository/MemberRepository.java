package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import study.querydsl.entity.Member;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom, QuerydslPredicateExecutor<Member> {

    // 메소드 쿼리
    List<Member> findByUsername(String username);

    /**
     * QuerydslPredicateExecutor<Type>을 상속받으면 spring data JPA가 제공하는 인터페이스에 querydsl predicate querydsl의 조건을 넣을 수 있음
     *   * 조인 X (묵시적 조인은 가능하지만 left join이 불가능)
     *   * 클라이언트가 Querydsl에 의존해야 한다. 서비스 클래스가 Querydsl이라는 구현 기술에 의존해야 한다.
     *   * 복잡한 실무환경에서 사용하기에는 한계가 명확하다.
     *     * QuerydslPredicateExecutor는 pageable, Sort를 모두 지원하고 정상 동작한다
     */

}
