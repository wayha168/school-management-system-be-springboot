package com.project.school_management.service.school;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.project.school_management.dto.school.SchoolImage;
import com.project.school_management.dto.school.SchoolRequest;
import com.project.school_management.dto.school.SchoolResponse;
import com.project.school_management.entities.SchoolMag;
import com.project.school_management.exception.ExceptionNotFound;
import com.project.school_management.repository.SchoolRepository;
import com.project.school_management.security.SchoolScopeService;

@Service
@Transactional
public class SchoolServiceImpl implements SchoolService {

    private static final long MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif", "image/svg+xml");

    private final SchoolRepository schoolRepository;
    private final SchoolScopeService schoolScopeService;

    public SchoolServiceImpl(SchoolRepository schoolRepository, SchoolScopeService schoolScopeService) {
        this.schoolRepository = schoolRepository;
        this.schoolScopeService = schoolScopeService;
    }

    @Override
    public SchoolResponse create(SchoolRequest request) {
        return create(request, null, null);
    }

    @Override
    public SchoolResponse create(SchoolRequest request, MultipartFile logo, MultipartFile banner) {
        SchoolMag school = mapFields(new SchoolMag(), request);
        applyLogo(school, logo, request.getLogoBase64(), false);
        applyBanner(school, banner, request.getBannerBase64(), false);
        return SchoolResponse.from(schoolRepository.save(school));
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolResponse getById(UUID id) {
        schoolScopeService.assertSchoolAccess(id);
        return SchoolResponse.from(findSchool(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchoolResponse> getAll() {
        return schoolScopeService.scopedSchoolUuid()
                .map(schoolUuid -> List.of(getById(schoolUuid)))
                .orElseGet(() -> schoolRepository.findAll().stream().map(SchoolResponse::from).toList())
                .stream()
                .sorted(java.util.Comparator.comparing(
                        s -> s.getName() == null ? "" : s.getName(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public SchoolResponse update(UUID id, SchoolRequest request) {
        return update(id, request, null, null);
    }

    @Override
    public SchoolResponse update(UUID id, SchoolRequest request, MultipartFile logo, MultipartFile banner) {
        SchoolMag school = mapFields(findSchool(id), request);
        applyLogo(school, logo, request.getLogoBase64(), true);
        applyBanner(school, banner, request.getBannerBase64(), true);
        return SchoolResponse.from(schoolRepository.save(school));
    }

    @Override
    public void delete(UUID id) {
        if (!schoolRepository.existsById(id)) {
            throw new ExceptionNotFound("School not found: " + id);
        }
        schoolRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolImage getLogo(UUID id) {
        schoolScopeService.assertSchoolAccess(id);
        SchoolMag school = findSchool(id);
        if (!school.hasLogo()) {
            throw new ExceptionNotFound("Logo not found for school: " + id);
        }
        return new SchoolImage(school.getLogoData(), contentTypeOrDefault(school.getLogoContentType()));
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolImage getBanner(UUID id) {
        schoolScopeService.assertSchoolAccess(id);
        SchoolMag school = findSchool(id);
        if (!school.hasBanner()) {
            throw new ExceptionNotFound("Banner not found for school: " + id);
        }
        return new SchoolImage(school.getBannerData(), contentTypeOrDefault(school.getBannerContentType()));
    }

    private SchoolMag findSchool(UUID id) {
        return schoolRepository.findById(id)
                .orElseThrow(() -> new ExceptionNotFound("School not found: " + id));
    }

    private SchoolMag mapFields(SchoolMag school, SchoolRequest request) {
        school.setName(request.getName());
        school.setDescription(request.getDescription());
        school.setAddress(request.getAddress());
        school.setPhone(request.getPhone());
        school.setEmail(request.getEmail());
        school.setWebsite(request.getWebsite());
        return school;
    }

    private void applyLogo(SchoolMag school, MultipartFile file, String base64, boolean keepExisting) {
        ImagePayload payload = resolveImage(file, base64);
        if (payload != null) {
            school.setLogoData(payload.data());
            school.setLogoContentType(payload.contentType());
        } else if (!keepExisting) {
            school.setLogoData(null);
            school.setLogoContentType(null);
        }
    }

    private void applyBanner(SchoolMag school, MultipartFile file, String base64, boolean keepExisting) {
        ImagePayload payload = resolveImage(file, base64);
        if (payload != null) {
            school.setBannerData(payload.data());
            school.setBannerContentType(payload.contentType());
        } else if (!keepExisting) {
            school.setBannerData(null);
            school.setBannerContentType(null);
        }
    }

    private ImagePayload resolveImage(MultipartFile file, String base64) {
        if (file != null && !file.isEmpty()) {
            return fromMultipart(file);
        }
        if (base64 != null && !base64.isBlank()) {
            return fromBase64(base64.trim());
        }
        return null;
    }

    private ImagePayload fromMultipart(MultipartFile file) {
        String contentType = normalizeContentType(file.getContentType());
        validateContentType(contentType);
        try {
            byte[] data = file.getBytes();
            validateSize(data.length);
            return new ImagePayload(data, contentType);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to read uploaded image", ex);
        }
    }

    private ImagePayload fromBase64(String value) {
        String contentType = "image/png";
        String encoded = value;
        if (value.startsWith("data:")) {
            int comma = value.indexOf(',');
            if (comma < 0) {
                throw new IllegalArgumentException("Invalid image data URL");
            }
            String header = value.substring(5, comma);
            int semi = header.indexOf(';');
            contentType = normalizeContentType(semi >= 0 ? header.substring(0, semi) : header);
            encoded = value.substring(comma + 1);
        }
        validateContentType(contentType);
        try {
            byte[] data = Base64.getDecoder().decode(encoded);
            validateSize(data.length);
            return new ImagePayload(data, contentType);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid base64 image data", ex);
        }
    }

    private static void validateContentType(String contentType) {
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Only JPEG, PNG, WEBP, or GIF images are allowed");
        }
    }

    private static void validateSize(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Image file is empty");
        }
        if (length > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Image must be 5MB or smaller");
        }
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "image/png";
        }
        String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        return "image/jpg".equals(normalized) ? "image/jpeg" : normalized;
    }

    private static String contentTypeOrDefault(String contentType) {
        return contentType == null || contentType.isBlank() ? "image/png" : contentType;
    }

    private record ImagePayload(byte[] data, String contentType) {
    }
}
