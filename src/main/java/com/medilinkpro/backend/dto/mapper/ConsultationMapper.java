package com.medilinkpro.backend.dto.mapper;

import com.medilinkpro.backend.dto.response.ConsultationResponse;
import com.medilinkpro.backend.entity.Consultation;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ConsultationMapper {

    @Mapping(target = "dossierMedicalId", source = "dossierMedical.id")
    @Mapping(target = "medecinId", source = "medecin.id")
    @Mapping(target = "medecinNomComplet", expression = "java(c.getMedecin().getPrenom() + \" \" + c.getMedecin().getNom())")
    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "patientNomComplet", expression = "java(c.getPatient().getPrenom() + \" \" + c.getPatient().getNom())")
    @Mapping(target = "ordonnanceId", source = "ordonnance.id")
    ConsultationResponse toResponse(Consultation c);
}
