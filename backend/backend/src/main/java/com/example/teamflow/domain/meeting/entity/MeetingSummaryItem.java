package com.example.teamflow.domain.meeting.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "meeting_summary_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingSummaryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    public static MeetingSummaryItem create(Meeting meeting, int orderIndex, String content) {
        MeetingSummaryItem item = new MeetingSummaryItem();
        item.meeting = meeting;
        item.orderIndex = orderIndex;
        item.content = content;
        return item;
    }
}
