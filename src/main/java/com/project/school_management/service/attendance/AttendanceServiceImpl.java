package com.project.school_management.service.attendance;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.school_management.dto.attendance.AttendanceBatchRequest;
import com.project.school_management.dto.attendance.AttendanceMarkItem;
import com.project.school_management.dto.attendance.AttendanceRequest;
import com.project.school_management.dto.attendance.AttendanceResponse;
import com.project.school_management.dto.attendance.ClassAttendanceOverview;
import com.project.school_management.dto.dashboard.ChartSeries;
import com.project.school_management.dto.schoolclass.SchoolClassResponse;
import com.project.school_management.dto.user.DataUser;
import com.project.school_management.dto.user.UserClassItem;
import com.project.school_management.dto.user.UserResponse;
import com.project.school_management.entities.AttendanceRecord;
import com.project.school_management.entities.SchoolClass;
import com.project.school_management.entities.User;
import com.project.school_management.enums.AttendanceStatus;
import com.project.school_management.enums.RoleName;
import com.project.school_management.exception.ExceptionNotFound;
import com.project.school_management.repository.AttendanceRecordRepository;
import com.project.school_management.repository.SchoolClassRepository;
import com.project.school_management.repository.UserRepository;
import com.project.school_management.security.SchoolScopeService;
import com.project.school_management.service.schoolclass.SchoolClassService;
import com.project.school_management.service.user.UserService;

@Service
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRecordRepository attendanceRepository;
    private final UserRepository userRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolScopeService schoolScopeService;
    private final SchoolClassService schoolClassService;
    private final UserService userService;

    public AttendanceServiceImpl(
            AttendanceRecordRepository attendanceRepository,
            UserRepository userRepository,
            SchoolClassRepository schoolClassRepository,
            SchoolScopeService schoolScopeService,
            SchoolClassService schoolClassService,
            UserService userService) {
        this.attendanceRepository = attendanceRepository;
        this.userRepository = userRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.schoolScopeService = schoolScopeService;
        this.schoolClassService = schoolClassService;
        this.userService = userService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> list(LocalDate date, UUID classUuid, UUID userUuid) {
        User current = schoolScopeService.requireCurrentUser();
        RoleName role = current.getRole() != null ? current.getRole().getName() : null;
        UUID scopedUser = userUuid;
        if (role == RoleName.STUDENT) {
            scopedUser = current.getUuid();
        }
        UUID finalUser = scopedUser;
        boolean filterDate = date != null;
        boolean filterClass = classUuid != null;
        boolean filterUser = scopedUser != null;
        return attendanceRepository.findFiltered(
                filterDate, filterDate ? date : LocalDate.EPOCH,
                filterClass, filterClass ? classUuid : current.getUuid(),
                filterUser, filterUser ? scopedUser : current.getUuid())
                .stream()
                .filter(this::inScope)
                .filter(a -> role != RoleName.TEACHER || teachesOrSelf(current, a, finalUser))
                .map(AttendanceResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> listMine(LocalDate date) {
        User current = schoolScopeService.requireCurrentUser();
        return list(date, null, current.getUuid());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> listFiltered(
            LocalDate date,
            UUID classUuid,
            UUID userUuid,
            RoleName roleFilter,
            Integer generation) {
        List<AttendanceResponse> records = list(date, classUuid, userUuid);
        if (roleFilter != null) {
            records = records.stream()
                    .filter(r -> r.getUserRole() == roleFilter)
                    .toList();
        }
        if (generation != null) {
            records = records.stream()
                    .filter(r -> generation.equals(r.getGeneration()))
                    .toList();
        }
        return records;
    }

    @Override
    public AttendanceResponse upsert(AttendanceRequest request) {
        User marker = schoolScopeService.requireCurrentUser();
        User user = userRepository.findDetailedById(request.getUserUuid())
                .orElseThrow(() -> new ExceptionNotFound("User not found: " + request.getUserUuid()));
        SchoolClass schoolClass = null;
        if (request.getClassUuid() != null) {
            schoolClass = schoolClassRepository.findDetailedById(request.getClassUuid())
                    .orElseThrow(() -> new ExceptionNotFound("Class not found: " + request.getClassUuid()));
            assertCanMarkClass(marker, schoolClass);
            if (schoolClass.getSchool() != null) {
                schoolScopeService.assertSchoolAccess(schoolClass.getSchool().getUuid());
            }
        } else {
            assertStaffOrAdmin(marker);
            if (user.getSchool() != null) {
                schoolScopeService.assertSchoolAccess(user.getSchool().getUuid());
            }
        }

        AttendanceRecord record = findOrCreate(user, schoolClass, request.getAttendanceDate());
        record.setUser(user);
        record.setSchoolClass(schoolClass);
        record.setAttendanceDate(request.getAttendanceDate());
        record.setStatus(request.getStatus());
        record.setRemark(blankToNull(request.getRemark()));
        record.setMarkedBy(marker);
        return AttendanceResponse.from(attendanceRepository.save(record));
    }

    @Override
    public int markBatch(AttendanceBatchRequest request) {
        User marker = schoolScopeService.requireCurrentUser();
        SchoolClass schoolClass = schoolClassRepository.findDetailedById(request.getClassUuid())
                .orElseThrow(() -> new ExceptionNotFound("Class not found: " + request.getClassUuid()));
        assertCanMarkClass(marker, schoolClass);
        int count = 0;
        for (AttendanceMarkItem item : request.getItems()) {
            AttendanceRequest single = new AttendanceRequest();
            single.setUserUuid(item.getUserUuid());
            single.setClassUuid(request.getClassUuid());
            single.setAttendanceDate(request.getAttendanceDate());
            single.setStatus(item.getStatus());
            single.setRemark(item.getRemark());
            upsert(single);
            count++;
        }
        return count;
    }

    @Override
    public int markFromForm(
            UUID classUuid,
            LocalDate attendanceDate,
            List<UUID> studentUuids,
            List<AttendanceStatus> statuses) {
        if (classUuid == null) {
            throw new IllegalArgumentException("Class is required");
        }
        if (attendanceDate == null) {
            throw new IllegalArgumentException("Attendance date is required");
        }
        if (studentUuids == null || studentUuids.isEmpty()) {
            throw new IllegalArgumentException("No students submitted");
        }
        AttendanceBatchRequest batch = new AttendanceBatchRequest();
        batch.setClassUuid(classUuid);
        batch.setAttendanceDate(attendanceDate);
        List<AttendanceMarkItem> items = new ArrayList<>();
        for (int i = 0; i < studentUuids.size(); i++) {
            AttendanceMarkItem item = new AttendanceMarkItem();
            item.setUserUuid(studentUuids.get(i));
            item.setStatus(
                    statuses != null && i < statuses.size() && statuses.get(i) != null
                            ? statuses.get(i)
                            : AttendanceStatus.PRESENT);
            items.add(item);
        }
        batch.setItems(items);
        return markBatch(batch);
    }

    @Override
    public void delete(UUID id) {
        AttendanceRecord record = attendanceRepository.findDetailedById(id)
                .orElseThrow(() -> new ExceptionNotFound("Attendance not found: " + id));
        User current = schoolScopeService.requireCurrentUser();
        if (record.getSchoolClass() != null) {
            assertCanMarkClass(current, record.getSchoolClass());
        } else {
            assertStaffOrAdmin(current);
        }
        attendanceRepository.delete(record);
    }

    @Override
    @Transactional(readOnly = true)
    public ChartSeries lastMonthChart(int days) {
        return buildMonthChart(list(null, null, null), days);
    }

    @Override
    @Transactional(readOnly = true)
    public ChartSeries classMonthChart(UUID classUuid, int days) {
        return buildMonthChart(list(null, classUuid, null), days);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchoolClassResponse> visibleClasses(DataUser account) {
        List<SchoolClassResponse> all = schoolClassService.getAll();
        if (account == null || account.getRole() == null) {
            return List.of();
        }
        if (isManagement(account) || account.getRole() == RoleName.PRINCIPAL || account.getRole() == RoleName.STAFF) {
            return all;
        }
        if (account.getRole() == RoleName.TEACHER) {
            return markableClasses(account);
        }
        if (account.getRole() == RoleName.STUDENT && account.getClasses() != null) {
            Set<UUID> mine = account.getClasses().stream()
                    .map(UserClassItem::getUuid)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            return all.stream().filter(c -> mine.contains(c.getUuid())).toList();
        }
        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchoolClassResponse> visibleClasses(DataUser account, Integer generation) {
        List<SchoolClassResponse> classes = visibleClasses(account);
        if (generation == null) {
            return classes;
        }
        return classes.stream()
                .filter(c -> generation.equals(c.getGeneration()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchoolClassResponse> markableClasses(DataUser account) {
        List<SchoolClassResponse> all = schoolClassService.getAll();
        if (account == null || account.getRole() == null) {
            return List.of();
        }
        if (account.getRole() == RoleName.SUPERADMIN || account.getRole() == RoleName.ADMIN) {
            return all;
        }
        if (account.getRole() == RoleName.TEACHER) {
            if (account.getClasses() == null || account.getClasses().isEmpty()) {
                return List.of();
            }
            Set<UUID> taught = account.getClasses().stream()
                    .map(UserClassItem::getUuid)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            return all.stream().filter(c -> taught.contains(c.getUuid())).toList();
        }
        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolClassResponse findVisibleClass(DataUser account, UUID classUuid) {
        if (classUuid == null) {
            return null;
        }
        return visibleClasses(account).stream()
                .filter(c -> classUuid.equals(c.getUuid()))
                .findFirst()
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassAttendanceOverview> buildClassOverviews(List<SchoolClassResponse> classes) {
        if (classes == null || classes.isEmpty()) {
            return List.of();
        }
        List<UserResponse> allUsers = userService.getAll();
        List<ClassAttendanceOverview> rows = new ArrayList<>();
        for (SchoolClassResponse c : classes) {
            List<String> teacherNames = allUsers.stream()
                    .filter(u -> u.getRole() == RoleName.TEACHER)
                    .filter(u -> u.getClasses() != null
                            && u.getClasses().stream().anyMatch(cl -> c.getUuid().equals(cl.getUuid())))
                    .map(UserResponse::getName)
                    .filter(Objects::nonNull)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
            int studentCount = (int) allUsers.stream()
                    .filter(u -> u.getRole() == RoleName.STUDENT)
                    .filter(u -> u.getClasses() != null
                            && u.getClasses().stream().anyMatch(cl -> c.getUuid().equals(cl.getUuid())))
                    .count();
            String teachersLabel = teacherNames.isEmpty() ? "Unassigned" : String.join(", ", teacherNames);
            rows.add(ClassAttendanceOverview.builder()
                    .classUuid(c.getUuid())
                    .className(c.getName())
                    .grade(c.getGrade())
                    .generation(c.getGeneration())
                    .generationCode(c.getGenerationCode())
                    .schoolName(c.getSchoolName())
                    .teacherNames(teacherNames)
                    .teachersLabel(teachersLabel)
                    .studentCount(studentCount)
                    .build());
        }
        rows.sort(Comparator.comparing(
                r -> r.getClassName() == null ? "" : r.getClassName(),
                String.CASE_INSENSITIVE_ORDER));
        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> studentsInClass(UUID classUuid) {
        if (classUuid == null) {
            return List.of();
        }
        return userService.getAll().stream()
                .filter(u -> u.getRole() == RoleName.STUDENT)
                .filter(u -> u.getClasses() != null
                        && u.getClasses().stream().anyMatch(c -> classUuid.equals(c.getUuid())))
                .sorted(Comparator.comparing(
                        u -> u.getName() == null ? "" : u.getName(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> teachersForClass(UUID classUuid) {
        if (classUuid == null) {
            return List.of();
        }
        return userService.getAll().stream()
                .filter(u -> u.getRole() == RoleName.TEACHER)
                .filter(u -> u.getClasses() != null
                        && u.getClasses().stream().anyMatch(c -> classUuid.equals(c.getUuid())))
                .sorted(Comparator.comparing(
                        u -> u.getName() == null ? "" : u.getName(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public boolean canMark(DataUser account) {
        if (account == null || account.getRole() == null) {
            return false;
        }
        return account.getRole() == RoleName.SUPERADMIN
                || account.getRole() == RoleName.ADMIN
                || account.getRole() == RoleName.TEACHER;
    }

    @Override
    public boolean isManagement(DataUser account) {
        return account != null
                && (account.getRole() == RoleName.SUPERADMIN || account.getRole() == RoleName.ADMIN);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, AttendanceResponse> attendanceByUser(LocalDate date, UUID classUuid) {
        LocalDate day = date != null ? date : LocalDate.now();
        if (classUuid == null) {
            return Map.of();
        }
        return list(day, classUuid, null).stream()
                .filter(r -> r.getUserUuid() != null)
                .collect(Collectors.toMap(
                        AttendanceResponse::getUserUuid,
                        r -> r,
                        (a, b) -> a,
                        LinkedHashMap::new));
    }

    @Override
    public String resolveActiveTab(String tab, String viewAlias, DataUser account) {
        String view = tab != null && !tab.isBlank() ? tab : viewAlias;
        if ("records".equalsIgnoreCase(view)) {
            return "records";
        }
        if ("classes".equalsIgnoreCase(view)) {
            return "classes";
        }
        if (isManagement(account) || (account != null && account.getRole() == RoleName.TEACHER)) {
            return "classes";
        }
        return "records";
    }

    @Override
    public RoleName parseRole(String role) {
        if (role == null || role.isBlank() || "ALL".equalsIgnoreCase(role)) {
            return null;
        }
        try {
            return RoleName.valueOf(role.trim().toUpperCase());
        } catch (Exception ex) {
            return null;
        }
    }

    private ChartSeries buildMonthChart(List<AttendanceResponse> rows, int days) {
        int window = Math.max(1, Math.min(days, 90));
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(window - 1L);
        Map<LocalDate, int[]> counters = new LinkedHashMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            counters.put(d, new int[] { 0, 0, 0 });
        }
        for (AttendanceResponse row : rows) {
            if (row.getAttendanceDate() == null
                    || row.getAttendanceDate().isBefore(start)
                    || row.getAttendanceDate().isAfter(end)) {
                continue;
            }
            int[] c = counters.get(row.getAttendanceDate());
            if (c == null) {
                continue;
            }
            AttendanceStatus status = row.getStatus();
            if (status == AttendanceStatus.PRESENT) {
                c[0]++;
            } else if (status == AttendanceStatus.ABSENT) {
                c[1]++;
            } else if (status == AttendanceStatus.LATE || status == AttendanceStatus.EXCUSED) {
                c[2]++;
            }
        }
        List<String> labels = new ArrayList<>();
        List<Number> present = new ArrayList<>();
        List<Number> absent = new ArrayList<>();
        List<Number> late = new ArrayList<>();
        List<Number> totals = new ArrayList<>();
        for (Map.Entry<LocalDate, int[]> e : counters.entrySet()) {
            labels.add(e.getKey().getMonthValue() + "/" + e.getKey().getDayOfMonth());
            present.add(e.getValue()[0]);
            absent.add(e.getValue()[1]);
            late.add(e.getValue()[2]);
            totals.add(e.getValue()[0] + e.getValue()[1] + e.getValue()[2]);
        }
        return ChartSeries.builder()
                .labels(labels)
                .values(totals)
                .present(present)
                .absent(absent)
                .late(late)
                .build();
    }

    private AttendanceRecord findOrCreate(User user, SchoolClass schoolClass, LocalDate date) {
        if (schoolClass != null) {
            return attendanceRepository
                    .findByUser_UuidAndSchoolClass_UuidAndAttendanceDate(user.getUuid(), schoolClass.getUuid(), date)
                    .orElseGet(AttendanceRecord::new);
        }
        return attendanceRepository
                .findByUser_UuidAndSchoolClassIsNullAndAttendanceDate(user.getUuid(), date)
                .orElseGet(AttendanceRecord::new);
    }

    private boolean inScope(AttendanceRecord record) {
        if (record.getSchoolClass() != null && record.getSchoolClass().getSchool() != null) {
            return schoolScopeService.scopedSchoolUuid().isEmpty()
                    || schoolScopeService.scopedSchoolUuid().get()
                            .equals(record.getSchoolClass().getSchool().getUuid());
        }
        if (record.getUser() != null && record.getUser().getSchool() != null) {
            return schoolScopeService.scopedSchoolUuid().isEmpty()
                    || schoolScopeService.scopedSchoolUuid().get()
                            .equals(record.getUser().getSchool().getUuid());
        }
        return true;
    }

    private boolean teachesOrSelf(User teacher, AttendanceRecord record, UUID filterUser) {
        if (filterUser != null && filterUser.equals(teacher.getUuid())) {
            return true;
        }
        if (record.getSchoolClass() == null) {
            return false;
        }
        return teacher.getSchoolClasses() != null
                && teacher.getSchoolClasses().stream()
                        .anyMatch(c -> c.getUuid().equals(record.getSchoolClass().getUuid()));
    }

    private void assertCanMarkClass(User marker, SchoolClass schoolClass) {
        RoleName role = marker.getRole() != null ? marker.getRole().getName() : null;
        if (role == RoleName.SUPERADMIN || role == RoleName.ADMIN) {
            return;
        }
        if (role == RoleName.TEACHER) {
            boolean teaches = marker.getSchoolClasses() != null
                    && marker.getSchoolClasses().stream()
                            .anyMatch(c -> c.getUuid().equals(schoolClass.getUuid()));
            if (teaches) {
                return;
            }
        }
        throw new AccessDeniedException("You cannot mark attendance for this class");
    }

    private void assertStaffOrAdmin(User user) {
        RoleName role = user.getRole() != null ? user.getRole().getName() : null;
        if (role != RoleName.SUPERADMIN && role != RoleName.ADMIN) {
            throw new AccessDeniedException("Only admin/superadmin can mark attendance without a class");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
