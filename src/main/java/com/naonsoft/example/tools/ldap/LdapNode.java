package com.naonsoft.example.tools.ldap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.springframework.util.CollectionUtils;

import com.naonsoft.example.exception.LogicError;

public class LdapNode {

	private final LdapName dn;

	private final LdapAttribute attributes;

	/**
	 * LdapNode 클래스의 새 인스턴스를 초기화 합니다.
	 */
	public LdapNode(LdapName dn, LdapAttribute attributes) {

		if (dn == null) {
			throw new LogicError();
		}
		if (CollectionUtils.isEmpty(attributes)) {
			throw new LogicError();
		}
		this.dn = dn;
		this.attributes = attributes;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		LdapNode other = (LdapNode) obj;
		return Objects.equals(this.dn, other.dn);
	}

	public final LdapAttribute getAttributes() {

		return new LdapAttribute(this.attributes);
	}

	/**
	 * dn를 반환합니다.
	 * 
	 * @return dn
	 */
	public final LdapName getDn() {

		return this.dn;
	}

	public Optional<LdapName> getParentDn() {

		ArrayList<Rdn> rdns = new ArrayList<>(this.dn.getRdns());
		if (rdns.isEmpty()) {
			return Optional.empty();
		}
		rdns.remove(rdns.size() - 1);
		return Optional.of(new LdapName(rdns));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {

		return Objects.hash(this.dn);
	}

	public Set<String> keySet() {

		return this.attributes.keySet();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {

		StringBuilder builder = new StringBuilder();
		builder.append("Path : ").append(this.dn).append('\t');
		builder.append("Attributes : ");

		for (Entry<String, List<String>> entry : this.attributes.entrySet()) {
			builder.append('\t');
			builder.append(entry.getKey()).append(':').append(entry.getValue());
		}

		return builder.toString();
	}

}
