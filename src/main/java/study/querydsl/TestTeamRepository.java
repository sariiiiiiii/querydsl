package study.querydsl;

import org.springframework.data.jpa.repository.JpaRepository;
import study.querydsl.entity.Team;

public interface TestTeamRepository extends JpaRepository<Team, Long> {
}
