package com.example.ariaapi;

import com.example.ariaapi.entity.User;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserEntityTest {

    @Test
    void testUserCreation() {
        // 옵션 1: 인자 없는 기본 생성자를 사용하고 Setter로 필드 설정
        User user = new User(); // 인자 없는 생성자 호출
        user.setUsrNm("테스트이름");   // Setter를 사용하여 이름 설정
        user.setUsrTel("010-1234-5678"); // Setter를 사용하여 전화번호 설정
        user.setUsrBirth("1990-01-01");  // Setter를 사용하여 생년월일 설정

        assertEquals("테스트이름", user.getUsrNm());
        assertEquals("010-1234-5678", user.getUsrTel());
        assertEquals("1990-01-01", user.getUsrBirth());
    }

    // 필요한 경우 더 많은 테스트를 추가할 수 있습니다.
}