package com.project.school_management.service.school;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.project.school_management.dto.school.SchoolImage;
import com.project.school_management.dto.school.SchoolRequest;
import com.project.school_management.dto.school.SchoolResponse;

public interface SchoolService {

    SchoolResponse create(SchoolRequest request);

    SchoolResponse create(SchoolRequest request, MultipartFile logo, MultipartFile banner);

    SchoolResponse getById(UUID id);

    List<SchoolResponse> getAll();

    SchoolResponse update(UUID id, SchoolRequest request);

    SchoolResponse update(UUID id, SchoolRequest request, MultipartFile logo, MultipartFile banner);

    void delete(UUID id);

    SchoolImage getLogo(UUID id);

    SchoolImage getBanner(UUID id);
}
