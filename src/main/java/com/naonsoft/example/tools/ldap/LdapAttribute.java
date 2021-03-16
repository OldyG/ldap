/**
 * FileName : LdapAttribute.java
 * Created  : 2021. 3. 18.
 * Author   : hokkk
 * Summary  :
 * Copyright (C) 2021 Goldy Project Inc. All rights reserved.
 * 이 문서의 모든 저작권 및 지적 재산권은 Goldy Project에게 있습니다.
 * 이 문서의 어떠한 부분도 허가 없이 복제 또는 수정 하거나, 전송할 수 없습니다.
 */
package com.naonsoft.example.tools.ldap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LdapAttribute extends HashMap<String, List<String>> {

	private static final long serialVersionUID = 8656506863539603493L;

	private final Map<String, List<byte[]>> bytesAttr = new HashMap<>();

	private final Map<String, List<Object>> unknwonAttr = new HashMap<>();

	/**
	 * LdapAttribute 클래스의 새 인스턴스를 초기화 합니다.
	 */
	public LdapAttribute() {

		super();
	}

	public LdapAttribute(int initialCapacity) {

		super(initialCapacity);
	}

	public LdapAttribute(Map<String, List<String>> m) {

		super(m);
	}

	public final Map<String, List<byte[]>> getBytesAttr() {

		return new HashMap<>(this.bytesAttr);
	}

	public final Map<String, List<Object>> getUnknwonAttr() {

		return new HashMap<>(this.unknwonAttr);
	}

	/**
	 * {@inheritDoc}
	 */
	public String put(String key, String value) {

		List<String> before = super.get(key);
		if (before == null) {
			super.put(key, new ArrayList<>());
		}
		super.get(key).add(value);
		return value;
	}

	public void putByteArray(String key, List<byte[]> bytesAttr) {

		this.bytesAttr.put(key, bytesAttr);
	}

	public void putUnknown(String key, List<Object> unknownAttr) {

		this.unknwonAttr.put(key, unknownAttr);
	}

}
