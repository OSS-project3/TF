package com.example.teamflow.domain.member.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member_skill")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 50)
    private String skill;

    public static MemberSkill create(Member member, String skill) {
        MemberSkill ms = new MemberSkill();
        ms.member = member;
        ms.skill = skill;
        return ms;
    }
}
