package com.medilinkpro.backend.dto.mapper;

import com.medilinkpro.backend.dto.response.RendezVousResponse;
import com.medilinkpro.backend.entity.RendezVous;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface RendezVousMapper {

    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "patientNomComplet", expression = "java(rdv.getPatient().getPrenom() + \" \" + rdv.getPatient().getNom())")
    @Mapping(target = "medecinId", source = "medecin.id")
    @Mapping(target = "medecinNomComplet", expression = "java(rdv.getMedecin().getPrenom() + \" \" + rdv.getMedecin().getNom())")
    @Mapping(target = "specialiteMedecin", source = "medecin.specialite")
    @Mapping(target = "etablissementId", source = "etablissement.id")
    @Mapping(target = "etablissementNom", source = "etablissement.nom")
    RendezVousResponse toResponse(RendezVous rdv);
}
