package com.example.teamflow.domain.meeting.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "meeting_attendee",
        uniqueConstraints = @UniqueConstraint(columnNames = {"meeting_id", "member_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingAttendee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    public static MeetingAttendee create(Meeting meeting, Long memberId) {
        MeetingAttendee a = new MeetingAttendee();
        a.meeting = meeting;
        a.memberId = memberId;
        return a;
    }
}
