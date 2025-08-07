package com.example.ariaapi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_table", schema = "public")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usr_nm", nullable = false, length = 512)
    private String usrNm;

    @Column(name = "usr_tel", length = 512)
    private String usrTel;

    @Column(name = "usr_birth", length = 512)
    private String usrBirth;

    @Column(name = "usr_nm_hash", length = 64)
    private String usrNmHash;

    @Column(name = "usr_tel_hash", unique = true, length = 64)
    private String usrTelHash;

    @Column(name = "usr_birth_hash", length = 64)
    private String usrBirthHash;

}