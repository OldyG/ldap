/**
 * FileName : LdapConnection.java
 * Created  : 2021. 3. 18.
 * Author   : hokkk
 * Summary  :
 * Copyright (C) 2021 Goldy Project Inc. All rights reserved.
 * 이 문서의 모든 저작권 및 지적 재산권은 Goldy Project에게 있습니다.
 * 이 문서의 어떠한 부분도 허가 없이 복제 또는 수정 하거나, 전송할 수 없습니다.
 */
package com.naonsoft.example.tools.ldap;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.naonsoft.example.exception.LogicError;

public class LdapConnection {

	/**
	 * Slf4j Logger
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(LdapConnection.class);

	private InitialLdapContext ctx;

	private final Map<String, String> env;

	private boolean reconnect = false;

	private int reconnectCount = 0;

	private int sleepTime = 1000;

	private int reconnectLimit = 10;

	public LdapConnection(String host) {

		this(host, null, null);
	}

	public LdapConnection(String host, String adminId, String adminPassword) {

		if (StringUtils.isBlank(host)) {
			throw new IllegalArgumentException();
		}

		// adminId, adminPassword 둘중 한개만 공백인 경우 애러
		if (StringUtils.isAllBlank(adminId, adminPassword) == false) {
			if (StringUtils.isAnyBlank(adminId, adminPassword)) {
				throw new LogicError("계정 패스워드 정의가 올바르지 않습니다.");
			}
		}

		String finalUrl = "ldap://" + host.replace("ldap://", "");
		ConcurrentHashMap<String, String> tempMap = new ConcurrentHashMap<>();
		tempMap.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		tempMap.put(Context.PROVIDER_URL, finalUrl);

		// 계정 입력 여부에 따른 처리
		if (StringUtils.isBlank(adminId)) {
			tempMap.put(Context.SECURITY_AUTHENTICATION, "none");
		} else {
			tempMap.put(Context.SECURITY_AUTHENTICATION, "simple");
			tempMap.put(Context.SECURITY_PRINCIPAL, adminId);
			tempMap.put(Context.SECURITY_CREDENTIALS, adminPassword);
		}
		// env를 변경하지 못하도록 처리
		this.env = Collections.unmodifiableMap(tempMap);
		this.connect();
		this.disconnect();
	}

	public InitialLdapContext connect() {

		if (this.reconnect) {
			LOGGER.info("RECONNECTED : " + this.reconnectCount);
			if (this.reconnectCount > this.reconnectLimit) {
				throw new LogicError("재연결을 10회 시도하여 종료합니다.");
			}
			this.reconnectCount++;
			try {
				Thread.sleep(this.sleepTime);
				this.reconnect = false;
			} catch (InterruptedException e) {
				throw new LogicError(e);
			}
		}

		if (this.ctx != null) {
			this.disconnect();
		}

		try {
			this.ctx = new InitialLdapContext(new Hashtable<>(this.env), null);
		} catch (NamingException e) {
			throw new IllegalArgumentException("LDAP 연결 실패", e);
		}
		LOGGER.trace("Ldap Connected");
		return this.ctx;
	}

	public void disconnect() {

		this.reconnectCount = 0;
		if (this.ctx == null) {
			return;
		}
		try {
			this.ctx.close();
		} catch (NamingException e) {
			throw new LogicError("close 실패", e);
		}
		this.ctx = null;
		LOGGER.trace("Ldap Disconnected");
	}

	public void reconnect() {

		this.reconnect = true;
	}

	/**
	 * reconnectLimit 초기화 합니다.
	 * @param reconnectLimit 초기화 값
	 */
	public void setReconnectLimit(int limitReconnectCount) {

		this.reconnectLimit = limitReconnectCount;
	}

	/**
	 * sleepTime 초기화 합니다.
	 * @param sleepTime 초기화 값
	 */
	public void setSleepTime(int sleepTime) {

		this.sleepTime = sleepTime;
	}
}
