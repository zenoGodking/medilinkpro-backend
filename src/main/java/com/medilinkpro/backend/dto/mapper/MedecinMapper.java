package com.medilinkpro.backend.dto.mapper;

import com.medilinkpro.backend.dto.response.MedecinResponse;
import com.medilinkpro.backend.entity.Medecin;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MedecinMapper {

    @Mapping(target = "etablissementId", source = "etablissement.id")
    @Mapping(target = "etablissementNom", source = "etablissement.nom")
    @Mapping(target = "distanceApprox", ignore = true)
    MedecinResponse toResponse(Medecin medecin);
}
