package com.medilinkpro.backend.dto.mapper;

import com.medilinkpro.backend.dto.response.OrdonnanceResponse;
import com.medilinkpro.backend.entity.Ordonnance;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrdonnanceMapper {

    @Mapping(target = "consultationId", source = "consultation.id")
    @Mapping(target = "medecinId", source = "medecin.id")
    @Mapping(target = "medecinNomComplet", expression = "java(o.getMedecin().getPrenom() + \" \" + o.getMedecin().getNom())")
    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "patientNomComplet", expression = "java(o.getPatient().getPrenom() + \" \" + o.getPatient().getNom())")
    OrdonnanceResponse toResponse(Ordonnance o);
}
