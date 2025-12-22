package com.shu.backend.domain.reaction.dto;

import com.shu.backend.domain.reaction.enums.ReactionAction;
import com.shu.backend.domain.reaction.enums.ReactionTargetType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ReactionApplyRequest {

    @NotNull
    private ReactionTargetType targetType;

    @NotNull
    private Long targetId;

    @NotNull
    private ReactionAction action; // LIKE or DISLIKE
}