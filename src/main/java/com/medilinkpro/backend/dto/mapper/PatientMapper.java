package com.medilinkpro.backend.dto.mapper;

import com.medilinkpro.backend.dto.response.PatientResponse;
import com.medilinkpro.backend.entity.Patient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PatientMapper {

    @Mapping(target = "photoFacialeEnregistree", expression = "java(patient.getDescripteurFacial() != null)")
    PatientResponse toResponse(Patient patient);
}
