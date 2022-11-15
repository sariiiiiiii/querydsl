package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import study.querydsl.entity.Member;

@Data
@NoArgsConstructor
public class MemberDto {

    private String username;
    private int age;

    /**
     * @QUeryProjection annotation을 생성자에 적어주면
     * DTO class도 QType 클래스를 생성함
     */

//    @QueryProjection
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }

    public MemberDto(Member member) {
        this.username = member.getUsername();
        this.age = member.getAge();
    }

}
