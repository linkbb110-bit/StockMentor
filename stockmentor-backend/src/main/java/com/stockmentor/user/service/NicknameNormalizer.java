package com.stockmentor.user.service;

import com.stockmentor.common.exception.BusinessException;
import com.stockmentor.common.exception.ErrorCode;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class NicknameNormalizer {

    private static final int MIN_NICKNAME_LENGTH = 2;
    private static final int MAX_NICKNAME_LENGTH = 20;
    private static final Pattern NICKNAME_PATTERN =
            Pattern.compile("^[\\p{IsHan}A-Za-z0-9 _-]+$");

    public String normalize(String rawNickname) {
        if (rawNickname == null) {
            throw invalidNickname();
        }

        String normalizedNickname = rawNickname.trim();
        int normalizedCodePointCount =
                normalizedNickname.codePointCount(0, normalizedNickname.length());
        if (normalizedCodePointCount < MIN_NICKNAME_LENGTH
                || normalizedCodePointCount > MAX_NICKNAME_LENGTH
                || !NICKNAME_PATTERN.matcher(normalizedNickname).matches()) {
            throw invalidNickname();
        }
        return normalizedNickname;
    }

    private BusinessException invalidNickname() {
        return new BusinessException(ErrorCode.USER_NICKNAME_INVALID);
    }
}
