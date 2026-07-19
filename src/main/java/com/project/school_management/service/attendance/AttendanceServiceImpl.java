package com.project.school_management.service.attendance;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.school_management.dto.attendance.AttendanceBatchRequest;
import com.project.school_management.dto.attendance.AttendanceMarkItem;
import com.project.school_management.dto.attendance.AttendanceRequest;
import com.project.school_management.dto.attendance.AttendanceResponse;
import com.project.school_management.dto.dashboard.ChartSeries;
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

@Service
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRecordRepository attendanceRepository;
    private final UserRepository userRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolScopeService schoolScopeService;

    public AttendanceServiceImpl(
            AttendanceRecordRepository attendanceRepository,
            UserRepository userRepository,
            SchoolClassRepository schoolClassRepository,
            SchoolScopeService schoolScopeService) {
        this.attendanceRepository = attendanceRepository;
        this.userRepository = userRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.schoolScopeService = schoolScopeService;
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
                .filter(a -> inScope(a))
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
        // Management: Admin / Superadmin can mark any class
        if (role == RoleName.SUPERADMIN || role == RoleName.ADMIN) {
            return;
        }
        // Teachers assign attendance only for their own classes
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
