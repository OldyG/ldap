/**
 * FileName : LogicError.java
 * Created : 2019. 2. 26.
 * Author : jeonghyun.kum
 * Summary :
 * Copyright (C) 2021 Goldy Project Inc. All rights reserved.
 * 이 문서의 모든 저작권 및 지적 재산권은 Goldy Project에게 있습니다.
 * 이 문서의 어떠한 부분도 허가 없이 복제 또는 수정 하거나, 전송할 수 없습니다.
 */
package com.naonsoft.example.exception;

/**
 * 서버 로직 오류
 */
public class LogicError extends RuntimeException {

	/** Serial Version UID */
	private static final long serialVersionUID = -8539702088805302178L;

	/**
	 * {@link LogicError} 클래스의 새 인스턴스를 초기화 합니다.
	 *
	 * @author jeonghyun.kum
	 */
	public LogicError() {

		super("죄송합니다. 서버 내부적으로 오류가 발생하였습니다.");
	}

	/**
	 * {@link LogicError} 클래스의 새 인스턴스를 초기화 합니다.
	 *
	 * @author jeonghyun.kum
	 * @param message
	 *            오류 메세지
	 */
	public LogicError(String message) {

		super(message);
	}

	/**
	 * {@link LogicError} 클래스의 새 인스턴스를 초기화 합니다.
	 *
	 * @author jeonghyun.kum
	 * @param message
	 *            오류 메세지
	 * @param cause
	 *            cause
	 */
	public LogicError(String message, Throwable cause) {

		super(message, cause);
	}

	/**
	 * {@link LogicError} 클래스의 새 인스턴스를 초기화 합니다.
	 *
	 * @author 2019. 5. 6. 오후 5:11:53 jeonghyun.kum
	 * @param cause
	 *            the cause
	 */
	public LogicError(Throwable cause) {

		super(cause);

	}
}
