package com.example.ariaapi.dto.response;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDecryptedDto {
    private Long id; // User 엔티티의 ID (암호화되지 않음)
    private String usrNm; // 복호화된 사용자 이름 (또는 검색용 해시 후 복호화된 원본 이름)
    private String usrTel; // 복호화된 전화번호
    private String usrBirth; // 복호화된 생년월일
}
