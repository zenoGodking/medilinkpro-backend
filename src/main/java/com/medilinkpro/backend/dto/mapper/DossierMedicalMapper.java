package com.medilinkpro.backend.dto.mapper;

import com.medilinkpro.backend.dto.response.DossierMedicalResponse;
import com.medilinkpro.backend.dto.response.ResultatAnalyseResponse;
import com.medilinkpro.backend.entity.DossierMedical;
import com.medilinkpro.backend.entity.ResultatAnalyse;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = ConsultationMapper.class)
public interface DossierMedicalMapper {

    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "patientNomComplet", expression = "java(d.getPatient().getPrenom() + \" \" + d.getPatient().getNom())")
    DossierMedicalResponse toResponse(DossierMedical d);

    @Mapping(target = "dossierMedicalId", source = "dossierMedical.id")
    ResultatAnalyseResponse toResponse(ResultatAnalyse r);
}
