package com.example.iter.auth.exception;

import com.example.iter.common.exception.CustomException;
import com.example.iter.common.exception.ErrorCode;

public class RefreshTokenReuseException extends CustomException {

    public RefreshTokenReuseException() {
        super(ErrorCode.INVALID_REFRESH_TOKEN);
    }
}
