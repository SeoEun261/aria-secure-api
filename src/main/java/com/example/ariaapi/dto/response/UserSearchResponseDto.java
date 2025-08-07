package com.example.ariaapi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchResponseDto {
    private Long usrId;
    private String usrNm;
    private String usrTel;
    private String usrBirth;
}
