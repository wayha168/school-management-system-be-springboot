package com.project.assignment.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.assignment.dto.AssignmentRequest;
import com.project.assignment.dto.AssignmentResponse;
import com.project.assignment.dto.ClassroomDashboard;
import com.project.assignment.dto.MeetingRequest;
import com.project.assignment.dto.MeetingResponse;
import com.project.assignment.dto.SubmissionRequest;
import com.project.assignment.dto.SubmissionResponse;
import com.project.assignment.entity.Assignment;
import com.project.assignment.entity.AssignmentSubmission;
import com.project.assignment.entity.ClassMeeting;
import com.project.assignment.repository.AssignmentRepository;
import com.project.assignment.repository.AssignmentSubmissionRepository;
import com.project.assignment.repository.ClassMeetingRepository;
import com.project.assignment.security.CallerContext;
import com.project.assignment.security.CallerResolver;

@Service
@Transactional
public class ClassroomService {

    private final ClassMeetingRepository meetingRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final CallerResolver callerResolver;

    public ClassroomService(
            ClassMeetingRepository meetingRepository,
            AssignmentRepository assignmentRepository,
            AssignmentSubmissionRepository submissionRepository,
            CallerResolver callerResolver) {
        this.meetingRepository = meetingRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.callerResolver = callerResolver;
    }

    public MeetingResponse createMeeting(MeetingRequest request) {
        CallerContext caller = callerResolver.require();
        requireManager(caller);
        meetingRepository.deactivateAllForClass(request.getClassUuid());
        ClassMeeting meeting = new ClassMeeting();
        meeting.setClassUuid(request.getClassUuid());
        meeting.setTitle(request.getTitle().trim());
        meeting.setMeetingUrl(request.getMeetingUrl().trim());
        meeting.setProvider(normalizeProvider(request.getProvider(), request.getMeetingUrl()));
        meeting.setScheduledAt(request.getScheduledAt());
        meeting.setCreatedBy(caller.userUuid());
        meeting.setActive(true);
        return MeetingResponse.from(meetingRepository.save(meeting));
    }

    @Transactional(readOnly = true)
    public List<MeetingResponse> listMeetings(UUID classUuid) {
        callerResolver.require();
        return meetingRepository.findByClassUuidOrderByCreatedAtDesc(classUuid).stream()
                .map(MeetingResponse::from)
                .toList();
    }

    public MeetingResponse endMeeting(UUID meetingId) {
        CallerContext caller = callerResolver.require();
        requireManager(caller);
        ClassMeeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found: " + meetingId));
        meeting.setActive(false);
        return MeetingResponse.from(meetingRepository.save(meeting));
    }

    public AssignmentResponse createAssignment(AssignmentRequest request) {
        CallerContext caller = callerResolver.require();
        requireManager(caller);
        Assignment assignment = new Assignment();
        assignment.setClassUuid(request.getClassUuid());
        assignment.setTitle(request.getTitle().trim());
        assignment.setDescription(blankToNull(request.getDescription()));
        assignment.setDueAt(request.getDueAt());
        assignment.setCreatedBy(caller.userUuid());
        assignment.setStatus("OPEN");
        return AssignmentResponse.from(assignmentRepository.save(assignment));
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> listAssignments(UUID classUuid) {
        callerResolver.require();
        return assignmentRepository.findByClassUuidOrderByCreatedAtDesc(classUuid).stream()
                .map(AssignmentResponse::from)
                .toList();
    }

    public AssignmentResponse closeAssignment(UUID assignmentId) {
        CallerContext caller = callerResolver.require();
        requireManager(caller);
        Assignment assignment = findAssignment(assignmentId);
        assignment.setStatus("CLOSED");
        return AssignmentResponse.from(assignmentRepository.save(assignment));
    }

    public SubmissionResponse submit(UUID assignmentId, SubmissionRequest request) {
        CallerContext caller = callerResolver.require();
        if (!caller.isStudent() && !caller.canManageClassroom()) {
            throw new AccessDeniedException("Only students can submit assignments");
        }
        Assignment assignment = findAssignment(assignmentId);
        if (!"OPEN".equalsIgnoreCase(assignment.getStatus())) {
            throw new IllegalArgumentException("Assignment is closed");
        }
        UUID studentUuid = caller.userUuid();
        AssignmentSubmission submission = submissionRepository
                .findByAssignmentUuidAndStudentUuid(assignmentId, studentUuid)
                .orElseGet(AssignmentSubmission::new);
        submission.setAssignmentUuid(assignmentId);
        submission.setStudentUuid(studentUuid);
        submission.setContent(request.getContent().trim());
        submission.setSubmittedAt(java.time.LocalDateTime.now());
        return SubmissionResponse.from(submissionRepository.save(submission));
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> listSubmissions(UUID assignmentId) {
        CallerContext caller = callerResolver.require();
        requireManager(caller);
        findAssignment(assignmentId);
        return submissionRepository.findByAssignmentUuidOrderBySubmittedAtDesc(assignmentId).stream()
                .map(SubmissionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClassroomDashboard dashboard(List<UUID> classUuids) {
        callerResolver.require();
        List<UUID> ids = classUuids == null ? List.of() : classUuids.stream().filter(u -> u != null).distinct().toList();
        if (ids.isEmpty()) {
            return ClassroomDashboard.builder()
                    .activeMeetings(List.of())
                    .openAssignments(List.of())
                    .build();
        }
        List<MeetingResponse> meetings = meetingRepository
                .findByClassUuidInAndActiveTrueOrderByCreatedAtDesc(ids).stream()
                .map(MeetingResponse::from)
                .toList();
        List<AssignmentResponse> assignments = assignmentRepository
                .findByClassUuidInAndStatusOrderByDueAtAsc(ids, "OPEN").stream()
                .map(AssignmentResponse::from)
                .toList();
        return ClassroomDashboard.builder()
                .activeMeetings(meetings)
                .openAssignments(assignments)
                .build();
    }

    private Assignment findAssignment(UUID id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + id));
    }

    private static void requireManager(CallerContext caller) {
        if (!caller.canManageClassroom()) {
            throw new AccessDeniedException("Teachers or principals can manage classroom");
        }
    }

    private static String normalizeProvider(String provider, String url) {
        if (provider != null && !provider.isBlank()) {
            return provider.trim().toUpperCase(Locale.ROOT);
        }
        String lower = url == null ? "" : url.toLowerCase(Locale.ROOT);
        if (lower.contains("meet.google") || lower.contains("google.com/meet")) {
            return "MEET";
        }
        if (lower.contains("zoom.us") || lower.contains("zoom.com")) {
            return "ZOOM";
        }
        return "OTHER";
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
