package com.shu.backend.domain.poll.dto;

import com.shu.backend.domain.poll.entity.PollOption;
import com.shu.backend.global.util.HtmlText;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PollOptionResponse {
    private Long optionId;
    private String text;
    private int voteCount;
    private int percentage;
    private boolean selectedByMe;

    public static PollOptionResponse of(PollOption option, long totalVotes, Long selectedOptionId) {
        int percentage = totalVotes == 0
                ? 0
                : (int) Math.round(option.getVoteCount() * 100.0 / totalVotes);

        return PollOptionResponse.builder()
                .optionId(option.getId())
                .text(HtmlText.unescape(option.getText()))
                .voteCount(option.getVoteCount())
                .percentage(percentage)
                .selectedByMe(option.getId().equals(selectedOptionId))
                .build();
    }
}
