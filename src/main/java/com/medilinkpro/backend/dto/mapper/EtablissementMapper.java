package com.medilinkpro.backend.dto.mapper;

import com.medilinkpro.backend.dto.response.EtablissementResponse;
import com.medilinkpro.backend.entity.EtablissementSante;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EtablissementMapper {

    @org.mapstruct.Mapping(target = "directeurId", source = "directeur.id")
    @org.mapstruct.Mapping(target = "directeurNomComplet", expression = "java(etablissement.getDirecteur() == null ? null : etablissement.getDirecteur().getPrenom() + \" \" + etablissement.getDirecteur().getNom())")
    EtablissementResponse toResponse(EtablissementSante etablissement);
}
