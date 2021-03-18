/**
 * FileName : LdapService.java
 * Created  : 2021. 3. 18.
 * Author   : hokkk
 * Summary  :
 * Copyright (C) 2021 Goldy Project Inc. All rights reserved.
 * 이 문서의 모든 저작권 및 지적 재산권은 Goldy Project에게 있습니다.
 * 이 문서의 어떠한 부분도 허가 없이 복제 또는 수정 하거나, 전송할 수 없습니다.
 */
package com.naonsoft.example.tools.ldap;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InvalidSearchFilterException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import com.naonsoft.example.exception.LogicError;
import com.naonsoft.example.tools.tree.IsParentPredicate;
import com.naonsoft.example.tools.tree.Tree;
import com.naonsoft.example.tools.tree.TreeImpl;

/**
 * @author hokkk
 */
public class LdapService implements Closeable {

	private static final String ALL_SEARCH_FILTER = "(objectClass=*)";

	private final LdapConnection con;

	private final IsParentPredicate<LdapNode> isParentPredicate = (LdapNode parent, LdapNode target) -> {

		Optional<LdapName> optionalParent = getParent(target.getDn());
		if (optionalParent.isPresent() == false) {
			return false;
		}

		return parent.getDn().equals(optionalParent.get());
	};

	public LdapService(LdapConnection con) {

		if (con == null) {
			throw new NullPointerException("LdapConnection con is null");
		}

		this.con = con;
	}

	private static void appendAttribute(LdapAttribute result, Attribute attr) throws NamingException {

		List<String> stringAttr = new ArrayList<>();
		List<byte[]> bytesAttr = new ArrayList<>();
		List<Object> unknwonAttr = new ArrayList<>();
		NamingEnumeration<?> all = attr.getAll();
		while (all.hasMore()) {
			Object next = all.next();
			if (next == null) {
				continue;
			} else if (next instanceof String) {
				stringAttr.add((String) next);
			} else if (next instanceof byte[]) {
				bytesAttr.add((byte[]) next);
			} else {
				unknwonAttr.add(next);
			}
		}
		result.put(attr.getID(), stringAttr);
		if (bytesAttr.isEmpty() == false) {
			result.putByteArray(attr.getID(), bytesAttr);
		}
		if (unknwonAttr.isEmpty() == false) {
			result.putUnknown(attr.getID(), unknwonAttr);
		}
	}

	public static Optional<LdapName> getParent(LdapName dn) {

		ArrayList<Rdn> rdns = new ArrayList<>(dn.getRdns());
		if (rdns.isEmpty()) {
			return Optional.empty();
		}
		rdns.remove(rdns.size() - 1);
		return Optional.of(new LdapName(rdns));
	}

	private static boolean isTimeOutError(NamingException e) {

		return e.getMessage().contains("read timed out");
	}

	private static LdapName toName(String dn) {

		try {
			return new LdapName(dn);
		} catch (InvalidNameException e) {
			throw new LogicError("이름이 올바르지 않습니다 : " + dn, e);
		}
	}

	@Deprecated
	public List<Tree<LdapNode>> allTree() {

		List<LdapNode> roots = this.getRoots();

		List<Tree<LdapNode>> result = new ArrayList<>(roots.size());

		for (LdapNode root : roots) {
			Tree<LdapNode> rootTree = new TreeImpl<>(null, root);
			this.collectRecursiveChild(rootTree);
			result.add(rootTree);
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() {

		this.con.disconnect();
	}

	private void collectRecursiveChild(Tree<LdapNode> parent) {

		LdapName dn = parent.getData().getDn();

		List<LdapNode> childs = this.getChilds(dn);
		for (LdapNode ldapNode : childs) {
			this.collectRecursiveChild(parent.addChild(ldapNode));
		}
	}

	public LdapAttribute getAttr(LdapName dn) {

		InitialLdapContext ctx = this.con.connect();
		try {
			Attributes attr = ctx.getAttributes(dn);
			return this.toAttr(attr);
		} catch (NamingException e) {
			if (isTimeOutError(e)) {
				this.con.reconnect();
				return this.getAttr(dn);
			}
			throw new LogicError(e);
		} finally {
			this.con.disconnect();
		}
	}

	public LdapAttribute getAttr(String dn) {

		return this.getAttr(toName(dn));
	}

	public List<LdapNode> getChilds(LdapName dn) {

		NamingEnumeration<SearchResult> childs = this.searchOneLevel(dn, ALL_SEARCH_FILTER);
		List<LdapNode> result = new ArrayList<>();
		while (childs.hasMoreElements()) {
			SearchResult next = childs.nextElement();
			result.add(this.toNode(next));
		}
		return result;

	}

	public List<LdapNode> getChilds(String dn) {

		return this.getChilds(toName(dn));
	}

	public DirContext getDir(LdapName dn) {

		InitialLdapContext ctx = this.con.connect();
		try {
			return ctx.getSchemaClassDefinition(dn);
		} catch (NamingException e) {
			if (isTimeOutError(e)) {
				this.con.reconnect();
				return this.getDir(dn);
			}
			throw new LogicError(e);
		} finally {
			this.con.disconnect();
		}
	}

	public DirContext getDir(String dn) {

		return this.getDir(toName(dn));
	}

	public List<LdapNode> getRoots() {

		InitialLdapContext ctx = this.con.connect();
		try {
			NamingEnumeration<?> rootEnumertation = ctx
					.getAttributes("", new String[] { "namingContexts" })
					.get("namingContexts")
					.getAll();
			List<LdapNode> result = new ArrayList<>();

			while (rootEnumertation.hasMore()) {
				String path = (String) rootEnumertation.next();
				result.add(new LdapNode(toName(path), this.getAttr(path)));
			}
			return result;
		} catch (NamingException e) {
			if (isTimeOutError(e)) {
				this.con.reconnect();
				return this.getRoots();
			}
			throw new LogicError(e);
		} finally {
			this.con.disconnect();
		}
	}

	private NamingEnumeration<SearchResult> search(LdapName dn, String searchFilter, int scope) {

		InitialLdapContext ctx = this.con.connect();
		SearchControls searchControls = new SearchControls();
		searchControls.setSearchScope(scope);
		try {
			return ctx.search(dn, searchFilter, searchControls);
		} catch (InvalidSearchFilterException e) {
			throw new IllegalArgumentException(e);
		} catch (NamingException e) {
			if (isTimeOutError(e)) {
				this.con.reconnect();
				return this.searchOneLevel(dn, searchFilter);
			}
			throw new LogicError(e);
		} finally {
			this.con.disconnect();
		}
	}

	/**
	 * dn의 1레벨 자식들 대상으로 필터링
	 * 
	 * (cn=abc) => cn이 abc인것 반환
	 * (cn=abc*) => abc로 시작
	 * (&(cn=abc*)(cn=*d)) => AND 조건
	 * (|(cn=abc*)(cn=*d)) => OR 조건
	 */
	public NamingEnumeration<SearchResult> searchOneLevel(LdapName dn, String searchFilter) {

		return this.search(dn, searchFilter, SearchControls.ONELEVEL_SCOPE);
	}

	public NamingEnumeration<SearchResult> searchOneLevel(String dn, String searchFilter) {

		return this.searchOneLevel(toName(dn), searchFilter);
	}

	/**
	 * dn의 모든 자식들을 대상으로 필터링
	 */
	public NamingEnumeration<SearchResult> searchSubtree(LdapName dn, String searchFilter) {

		return this.search(dn, searchFilter, SearchControls.SUBTREE_SCOPE);
	}

	public NamingEnumeration<SearchResult> searchSubtree(String dn, String searchFilter) {

		return this.searchSubtree(toName(dn), searchFilter);
	}

	public LdapAttribute toAttr(Attributes attrs) {

		List<? extends Attribute> attrsList = Collections.list(attrs.getAll());

		LdapAttribute result = new LdapAttribute(attrsList.size());

		try {
			for (Attribute attr : attrsList) {

				appendAttribute(result, attr);
			}
			return result;
		} catch (NamingException e) {
			if (isTimeOutError(e)) {
				this.con.reconnect();
				return this.toAttr(attrs);
			}
			throw new LogicError(e);
		} finally {
			this.con.disconnect();
		}
	}

	public LdapNode toNode(LdapName dn) {

		LdapAttribute attr = this.getAttr(dn);
		return new LdapNode(dn, attr);
	}

	public LdapNode toNode(SearchResult searchResult) {

		LdapAttribute attributes = this.toAttr(searchResult.getAttributes());
		return new LdapNode(toName(searchResult.getNameInNamespace()), attributes);
	}

	public LdapNode toNode(String dn) {

		return this.toNode(toName(dn));
	}

	public Tree<LdapNode> tree(LdapName dn) {

		NamingEnumeration<SearchResult> searchResult = this.searchSubtree(dn, ALL_SEARCH_FILTER);

		List<LdapNode> nodes = Collections.list(searchResult)
				.stream()
				.map(this::toNode)
				.collect(Collectors.toList());

		if (nodes.isEmpty()) {
			throw new LogicError("nodes가 0인 경우 코드 검토가 필요합니다.");
		}

		// 한번에 가져오는 개수가 2,000개로 제한되어있으므로 넘어가는 경우 느린 검색을 수행한다.
		if (nodes.size() >= 2_000) {
			return this.treeSlow(dn);
		}

		List<Tree<LdapNode>> tree = TreeImpl.toTree(nodes, this.isParentPredicate);
		if (tree.size() != 1) {
			throw new LogicError("ID에 해당하는 하위 노드만 검색하였기때문에 반드시 한 개만 반환되어야 합니다.");
		}

		Tree<LdapNode> result = tree.get(0);

		result.sortRecursive(Comparator.comparing(LdapNode::getDn));
		return result;
	}

	public Tree<LdapNode> tree(String dn) {

		return this.tree(toName(dn));
	}

	public Tree<LdapNode> treeSlow(LdapName dn) {

		LdapNode rootNode = this.toNode(dn);
		Tree<LdapNode> rootTree = new TreeImpl<>(null, rootNode);
		this.collectRecursiveChild(rootTree);
		return rootTree;
	}

	public Tree<LdapNode> treeSlow(String dn) {

		return this.treeSlow(toName(dn));
	}

}
