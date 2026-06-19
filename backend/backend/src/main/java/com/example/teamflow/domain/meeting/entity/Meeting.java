package com.example.teamflow.domain.meeting.entity;

import com.example.teamflow.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meeting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Meeting extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private LocalDate date;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_manual", nullable = false)
    private boolean manual;

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<MeetingAttendee> attendees = new ArrayList<>();

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    private List<MeetingSummaryItem> summaryItems = new ArrayList<>();

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<MeetingTodo> todos = new ArrayList<>();

    public static Meeting create(String title, LocalDate date, String notes, boolean manual) {
        Meeting m = new Meeting();
        m.title = title;
        m.date = date;
        m.notes = notes;
        m.manual = manual;
        return m;
    }

    public void addAttendee(Long memberId) {
        attendees.add(MeetingAttendee.create(this, memberId));
    }

    public void addSummaryItem(int orderIndex, String content) {
        summaryItems.add(MeetingSummaryItem.create(this, orderIndex, content));
    }

    public void addTodo(Long assigneeId, Long projectId, String title, LocalDate dueDate) {
        todos.add(MeetingTodo.create(this, assigneeId, projectId, title, dueDate));
    }
}
