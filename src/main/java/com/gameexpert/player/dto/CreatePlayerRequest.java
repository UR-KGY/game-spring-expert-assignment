package com.gameexpert.player.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import org.springframework.validation.annotation.Validated;

@Getter
@Validated
public class CreatePlayerRequest {

    // TODO Lv 3: 2~12글자의 영문 대소문자, 숫자와 밑줄을 허용하는 검증을 적용합니다.
    @NotBlank
    @Size(min = 2,max = 12)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$")
    private final String nickname;

    public CreatePlayerRequest(String nickname) {
        this.nickname = nickname;
    }
}
