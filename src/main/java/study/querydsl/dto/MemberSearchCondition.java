package study.querydsl.dto;

import lombok.Data;

@Data
public class MemberSearchCondition {
    //회원명, 팀명, 나이(ageGoe, ageLoe)
    private String username;
    private String teamName;
    private Integer ageGoe; // null일 수도 있어서 Integer를 사용
    private Integer ageLoe;
}
