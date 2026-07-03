package com.medilinkpro.backend.dto.mapper;

import com.medilinkpro.backend.dto.response.PatientResponse;
import com.medilinkpro.backend.entity.Patient;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PatientMapper {

    PatientResponse toResponse(Patient patient);
}
