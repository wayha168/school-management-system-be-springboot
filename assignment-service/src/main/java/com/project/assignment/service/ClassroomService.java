package com.project.assignment.service;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

    private static final String ROOM_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ClassMeetingRepository meetingRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final CallerResolver callerResolver;
    private final RecordingStorageService recordingStorageService;
    private final AttachmentStorageService attachmentStorageService;

    public ClassroomService(
            ClassMeetingRepository meetingRepository,
            AssignmentRepository assignmentRepository,
            AssignmentSubmissionRepository submissionRepository,
            CallerResolver callerResolver,
            RecordingStorageService recordingStorageService,
            AttachmentStorageService attachmentStorageService) {
        this.meetingRepository = meetingRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.callerResolver = callerResolver;
        this.recordingStorageService = recordingStorageService;
        this.attachmentStorageService = attachmentStorageService;
    }

    public MeetingResponse createMeeting(MeetingRequest request) {
        CallerContext caller = callerResolver.require();
        requireManager(caller);
        meetingRepository.deactivateAllForClass(request.getClassUuid());

        String roomCode = allocateRoomCode();
        ClassMeeting meeting = new ClassMeeting();
        meeting.setClassUuid(request.getClassUuid());
        meeting.setTitle(request.getTitle().trim());
        meeting.setRoomCode(roomCode);
        meeting.setMeetingUrl(MeetingResponse.joinPathFor(roomCode));
        meeting.setProvider("NATIVE");
        meeting.setRecordEnabled(request.isRecordEnabled());
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

    @Transactional(readOnly = true)
    public MeetingResponse getByRoomCode(String roomCode) {
        callerResolver.require();
        return MeetingResponse.from(findByRoomCode(roomCode));
    }

    @Transactional(readOnly = true)
    public MeetingResponse getById(UUID meetingId) {
        callerResolver.require();
        return MeetingResponse.from(findMeeting(meetingId));
    }

    public MeetingResponse endMeeting(UUID meetingId) {
        CallerContext caller = callerResolver.require();
        requireManager(caller);
        ClassMeeting meeting = findMeeting(meetingId);
        meeting.setActive(false);
        return MeetingResponse.from(meetingRepository.save(meeting));
    }

    public MeetingResponse storeRecording(UUID meetingId, MultipartFile file) throws IOException {
        CallerContext caller = callerResolver.require();
        requireManager(caller);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Recording file is required");
        }
        ClassMeeting meeting = findMeeting(meetingId);
        if (!meeting.isRecordEnabled()) {
            throw new IllegalArgumentException("Recording is not enabled for this meeting");
        }
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.startsWith("video/") || contentType.equals("application/octet-stream"))) {
            throw new IllegalArgumentException("Only video recordings are accepted");
        }
        recordingStorageService.deleteIfPresent(meeting.getRecordingStoredName());
        String storedName = recordingStorageService.store(meetingId, file);
        meeting.setRecordingStoredName(storedName);
        meeting.setRecordingContentType(contentType.startsWith("video/") ? contentType : "video/webm");
        meeting.setRecordingBytes(file.getSize());
        meeting.setRecordedAt(LocalDateTime.now());
        meeting.setHasRecording(true);
        return MeetingResponse.from(meetingRepository.save(meeting));
    }

    @Transactional(readOnly = true)
    public RecordingPayload loadRecording(UUID meetingId) {
        callerResolver.require();
        ClassMeeting meeting = findMeeting(meetingId);
        if (!meeting.isHasRecording() || meeting.getRecordingStoredName() == null) {
            throw new IllegalArgumentException("No recording stored for this meeting");
        }
        Resource resource = recordingStorageService.load(meeting.getRecordingStoredName());
        return new RecordingPayload(
                resource,
                meeting.getRecordingContentType() != null ? meeting.getRecordingContentType() : "video/webm",
                meeting.getTitle() + "-recording.webm",
                meeting.getRecordingBytes());
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
        return submit(assignmentId, request != null ? request.getContent() : null, null);
    }

    public SubmissionResponse submit(UUID assignmentId, String content, MultipartFile file) {
        CallerContext caller = callerResolver.require();
        if (!caller.isStudent() && !caller.canManageClassroom()) {
            throw new AccessDeniedException("Only students can submit assignments");
        }
        Assignment assignment = findAssignment(assignmentId);
        if (!"OPEN".equalsIgnoreCase(assignment.getStatus())) {
            throw new IllegalArgumentException("Assignment is closed");
        }
        String text = content == null ? "" : content.trim();
        boolean hasFile = file != null && !file.isEmpty();
        if (text.isEmpty() && !hasFile) {
            throw new IllegalArgumentException("Add text or upload a file/image");
        }

        UUID studentUuid = caller.userUuid();
        AssignmentSubmission submission = submissionRepository
                .findByAssignmentUuidAndStudentUuid(assignmentId, studentUuid)
                .orElseGet(AssignmentSubmission::new);
        if (submission.getUuid() == null) {
            submission.setUuid(UUID.randomUUID());
        }
        submission.setAssignmentUuid(assignmentId);
        submission.setStudentUuid(studentUuid);
        submission.setContent(text.isEmpty() ? null : text);
        submission.setSubmittedAt(LocalDateTime.now());

        if (hasFile) {
            try {
                attachmentStorageService.deleteIfPresent(submission.getAttachmentStoredName());
                AttachmentStorageService.StoredFile stored =
                        attachmentStorageService.store(submission.getUuid(), file);
                submission.setHasAttachment(true);
                submission.setAttachmentStoredName(stored.storedName());
                submission.setAttachmentOriginalName(stored.originalName());
                submission.setAttachmentContentType(stored.contentType());
                submission.setAttachmentBytes(stored.bytes());
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to store attachment: " + ex.getMessage(), ex);
            }
        }

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
    public RecordingPayload loadSubmissionAttachment(UUID assignmentId, UUID submissionId) {
        CallerContext caller = callerResolver.require();
        AssignmentSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found"));
        if (!assignmentId.equals(submission.getAssignmentUuid())) {
            throw new IllegalArgumentException("Submission does not belong to this assignment");
        }
        boolean own = caller.userUuid().equals(submission.getStudentUuid());
        if (!own && !caller.canManageClassroom()) {
            throw new AccessDeniedException("Not allowed to download this attachment");
        }
        if (!submission.isHasAttachment() || submission.getAttachmentStoredName() == null) {
            throw new IllegalArgumentException("No attachment on this submission");
        }
        Resource resource = attachmentStorageService.load(submission.getAttachmentStoredName());
        String name = submission.getAttachmentOriginalName() != null
                ? submission.getAttachmentOriginalName()
                : "attachment";
        return new RecordingPayload(
                resource,
                submission.getAttachmentContentType() != null
                        ? submission.getAttachmentContentType()
                        : "application/octet-stream",
                name,
                submission.getAttachmentBytes());
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

    private ClassMeeting findMeeting(UUID id) {
        return meetingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found: " + id));
    }

    private ClassMeeting findByRoomCode(String roomCode) {
        if (roomCode == null || roomCode.isBlank()) {
            throw new IllegalArgumentException("Room code is required");
        }
        return meetingRepository.findByRoomCode(roomCode.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("Meeting room not found"));
    }

    private Assignment findAssignment(UUID id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + id));
    }

    private String allocateRoomCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String code = randomRoomCode(10);
            if (!meetingRepository.existsByRoomCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not allocate a unique room code");
    }

    private static String randomRoomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ROOM_ALPHABET.charAt(RANDOM.nextInt(ROOM_ALPHABET.length())));
        }
        return sb.toString();
    }

    private static void requireManager(CallerContext caller) {
        if (!caller.canManageClassroom()) {
            throw new AccessDeniedException("Teachers or principals can manage classroom");
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public record RecordingPayload(Resource resource, String contentType, String downloadName, Long size) {
    }
}
