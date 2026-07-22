package com.project.school_management.service.subject;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.school_management.dto.subject.SubjectRequest;
import com.project.school_management.dto.subject.SubjectResponse;
import com.project.school_management.entities.SchoolMag;
import com.project.school_management.entities.Subject;
import com.project.school_management.exception.ExceptionNotFound;
import com.project.school_management.repository.SchoolRepository;
import com.project.school_management.repository.SubjectRepository;
import com.project.school_management.security.SchoolScopeService;

@Service
@Transactional
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolScopeService schoolScopeService;

    public SubjectServiceImpl(
            SubjectRepository subjectRepository,
            SchoolRepository schoolRepository,
            SchoolScopeService schoolScopeService) {
        this.subjectRepository = subjectRepository;
        this.schoolRepository = schoolRepository;
        this.schoolScopeService = schoolScopeService;
    }

    @Override
    public SubjectResponse create(SubjectRequest request) {
        schoolScopeService.assertSchoolAccess(request.getSchoolUuid());
        String name = requireName(request.getName());
        assertUniqueName(request.getSchoolUuid(), name, null);
        String code = generateUniqueCode(request.getSchoolUuid(), name);

        Subject subject = new Subject();
        subject.setName(name);
        subject.setCode(code);
        subject.setDescription(blankToNull(request.getDescription()));
        subject.setSchool(findSchool(request.getSchoolUuid()));
        return SubjectResponse.from(subjectRepository.save(subject));
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectResponse getById(UUID id) {
        Subject subject = findSubject(id);
        if (subject.getSchool() != null) {
            schoolScopeService.assertSchoolAccess(subject.getSchool().getUuid());
        }
        return SubjectResponse.from(subject);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectResponse> getAll() {
        List<Subject> entities = schoolScopeService.scopedSchoolUuid()
                .map(subjectRepository::findDetailedBySchoolUuid)
                .orElseGet(subjectRepository::findAllDetailed);
        return entities.stream()
                .map(SubjectResponse::from)
                .sorted(Comparator.comparing(
                        s -> s.getName() == null ? "" : s.getName(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectResponse> getBySchool(UUID schoolUuid) {
        schoolScopeService.assertSchoolAccess(schoolUuid);
        return subjectRepository.findDetailedBySchoolUuid(schoolUuid).stream()
                .map(SubjectResponse::from)
                .toList();
    }

    @Override
    public SubjectResponse update(UUID id, SubjectRequest request) {
        Subject subject = findSubject(id);
        if (subject.getSchool() != null) {
            schoolScopeService.assertSchoolAccess(subject.getSchool().getUuid());
        }
        schoolScopeService.assertSchoolAccess(request.getSchoolUuid());

        String name = requireName(request.getName());
        assertUniqueName(request.getSchoolUuid(), name, id);

        // Keep existing auto code; only fill if missing
        String code = subject.getCode();
        if (code == null || code.isBlank()) {
            code = generateUniqueCode(request.getSchoolUuid(), name);
        }

        subject.setName(name);
        subject.setCode(code);
        subject.setDescription(blankToNull(request.getDescription()));
        subject.setSchool(findSchool(request.getSchoolUuid()));
        return SubjectResponse.from(subjectRepository.save(subject));
    }

    @Override
    public void delete(UUID id) {
        Subject subject = findSubject(id);
        if (subject.getSchool() != null) {
            schoolScopeService.assertSchoolAccess(subject.getSchool().getUuid());
        }
        subjectRepository.delete(subject);
    }

    private Subject findSubject(UUID id) {
        return subjectRepository.findDetailedById(id)
                .orElseThrow(() -> new ExceptionNotFound("Subject not found: " + id));
    }

    private SchoolMag findSchool(UUID id) {
        return schoolRepository.findById(id)
                .orElseThrow(() -> new ExceptionNotFound("School not found: " + id));
    }

    private void assertUniqueName(UUID schoolUuid, String name, UUID excludeId) {
        boolean taken = excludeId == null
                ? subjectRepository.existsBySchool_UuidAndNameIgnoreCase(schoolUuid, name)
                : subjectRepository.existsBySchoolAndNameExcluding(schoolUuid, name, excludeId);
        if (taken) {
            throw new IllegalArgumentException("Subject already exists for this school: " + name);
        }
    }

    private String generateUniqueCode(UUID schoolUuid, String name) {
        String base = buildCodeBase(name);
        String candidate = base;
        int suffix = 2;
        while (subjectRepository.existsBySchool_UuidAndCodeIgnoreCase(schoolUuid, candidate)) {
            String next = base + suffix;
            if (next.length() > 30) {
                next = base.substring(0, Math.max(1, 30 - String.valueOf(suffix).length())) + suffix;
            }
            candidate = next;
            suffix++;
            if (suffix > 9999) {
                throw new IllegalStateException("Unable to generate a unique subject code");
            }
        }
        return candidate;
    }

    /** Build a short uppercase code from the subject name (e.g. Physical Education → PE). */
    private static String buildCodeBase(String name) {
        String cleaned = name.trim().replaceAll("[^\\p{L}\\p{N}\\s]+", " ").trim();
        if (cleaned.isEmpty()) {
            return "SUBJ";
        }
        String[] parts = cleaned.split("\\s+");
        String base;
        if (parts.length >= 2) {
            StringBuilder initials = new StringBuilder();
            for (String part : parts) {
                if (part.isEmpty()) {
                    continue;
                }
                int cp = part.codePointAt(0);
                initials.appendCodePoint(Character.toUpperCase(cp));
                if (initials.length() >= 6) {
                    break;
                }
            }
            base = initials.toString();
        } else {
            String compact = parts[0].toUpperCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
            base = compact.length() <= 6 ? compact : compact.substring(0, 6);
        }
        if (base.isBlank()) {
            return "SUBJ";
        }
        return base.length() > 30 ? base.substring(0, 30) : base;
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Subject name is required");
        }
        return name.trim();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
