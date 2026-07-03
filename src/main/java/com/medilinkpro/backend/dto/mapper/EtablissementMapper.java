package com.medilinkpro.backend.dto.mapper;

import com.medilinkpro.backend.dto.response.EtablissementResponse;
import com.medilinkpro.backend.entity.EtablissementSante;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EtablissementMapper {

    EtablissementResponse toResponse(EtablissementSante etablissement);
}
