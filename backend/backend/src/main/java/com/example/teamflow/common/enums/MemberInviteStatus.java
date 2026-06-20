package com.example.teamflow.common.enums;

/**
 * 이메일 기반 팀원 초대(참가 요청)의 상태.
 * PM이 이메일로 초대를 보내면 PENDING 으로 생성되고,
 * 초대받은 멤버가 수락/거절하면 ACCEPTED/REJECTED 로 전이된다.
 */
public enum MemberInviteStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}
