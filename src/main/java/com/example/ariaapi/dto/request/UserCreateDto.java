package com.example.ariaapi.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateDto {
    private String usrNm; // 평문 사용자 이름
    private String usrTel; // 평문 전화번호
    private String usrBirth; // 평문 생년월일
}