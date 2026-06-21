package com.example.teamflow.domain.member.entity;

import com.example.teamflow.common.entity.BaseTimeEntity;
import com.example.teamflow.common.enums.MemberRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    @Column(nullable = false, length = 5)
    private String initial;

    @Column(nullable = false)
    private int weeklyCapacityHours;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    private Long workspaceId;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<MemberSkill> skills = new ArrayList<>();

    public static Member create(String name, MemberRole role, String initial,
                                int weeklyCapacityHours, String email, String password,
                                Long workspaceId) {
        Member member = new Member();
        member.name = name;
        member.role = role;
        member.initial = initial;
        member.weeklyCapacityHours = weeklyCapacityHours;
        member.email = email;
        member.password = password;
        member.workspaceId = workspaceId;
        return member;
    }

    public void addSkill(String skillName) {
        this.skills.add(MemberSkill.create(this, skillName));
    }

    public void updateProfile(String name, String initial, Integer weeklyCapacityHours) {
        if (name != null) this.name = name;
        if (initial != null) this.initial = initial;
        if (weeklyCapacityHours != null) this.weeklyCapacityHours = weeklyCapacityHours;
    }

    public void updateRole(MemberRole role) {
        if (role != null) this.role = role;
    }

    public void clearSkills() {
        this.skills.clear();
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }
}
