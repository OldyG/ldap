package com.naonsoft.example.tools.ldap;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.naming.Context;
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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.naonsoft.example.exception.LogicError;
import com.naonsoft.example.tools.tree.IsParentPredicate;
import com.naonsoft.example.tools.tree.Tree;
import com.naonsoft.example.tools.tree.TreeImpl;

public class LdapConnection implements Closeable {

	private static final String ALL_SEARCH_FILTER = "(objectClass=*)";

	private static final Logger LOGGER = LoggerFactory.getLogger(LdapConnection.class);

	private InitialLdapContext ctx;

	private final Map<String, String> env;

	private boolean reconnect = false;

	private int reconnectCount = 0;

	private int sleepTime = 1000;

	private int reconnectLimit = 10;

	private final IsParentPredicate<LdapNode> isParentPredicate = (LdapNode parent, LdapNode target) -> {

		Optional<LdapName> optionalParent = getParent(target.getDn());
		if (optionalParent.isPresent() == false) {
			return false;
		}

		return parent.getDn().equals(optionalParent.get());
	};

	/**
	 * LdapConnection 클래스의 새 인스턴스를 초기화 합니다.
	 */
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

	public static Optional<LdapName> getParent(LdapName current) {

		ArrayList<Rdn> rdns = new ArrayList<>(current.getRdns());
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

		this.disconnect();
	}

	private void collectRecursiveChild(Tree<LdapNode> parent) {

		LdapName dn = parent.getData().getDn();

		List<LdapNode> childs = this.getChilds(dn);
		for (LdapNode ldapNode : childs) {
			this.collectRecursiveChild(parent.addChild(ldapNode));
		}
	}

	private void connect() {

		if (this.reconnect) {
			LOGGER.info("RECONNECTED : " + this.reconnectCount);
			if (this.reconnectCount > this.reconnectLimit) {
				throw new LogicError("재앤결을 10회 시도하였습니다.");
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
	}

	private void disconnect() {

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

	}

	public LdapAttribute getAttr(LdapName dn) {

		this.connect();
		try {
			Attributes attr = this.ctx.getAttributes(dn);
			return this.toAttr(attr);
		} catch (NamingException e) {
			if (isTimeOutError(e)) {
				this.reconnect = true;
				return this.getAttr(dn);
			}
			throw new LogicError(e);
		} finally {
			this.disconnect();
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

		this.connect();
		try {
			return this.ctx.getSchemaClassDefinition(dn);
		} catch (NamingException e) {
			if (isTimeOutError(e)) {
				this.reconnect = true;
				return this.getDir(dn);
			}
			throw new LogicError(e);
		} finally {
			this.disconnect();
		}
	}

	public DirContext getDir(String dn) {

		return this.getDir(toName(dn));
	}

	public List<LdapNode> getRoots() {

		this.connect();
		try {
			NamingEnumeration<?> rootEnumertation = this.ctx
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
				this.reconnect = true;
				return this.getRoots();
			}
			throw new LogicError(e);
		} finally {
			this.disconnect();
		}
	}

	private NamingEnumeration<SearchResult> search(LdapName dn, String searchFilter, int scope) {

		this.connect();
		SearchControls searchControls = new SearchControls();
		searchControls.setSearchScope(scope);
		try {
			return this.ctx.search(dn, searchFilter, searchControls);
		} catch (InvalidSearchFilterException e) {
			throw new IllegalArgumentException(e);
		} catch (NamingException e) {
			if (isTimeOutError(e)) {
				this.reconnect = true;
				return this.searchOneLevel(dn, searchFilter);
			}
			throw new LogicError(e);
		} finally {
			this.disconnect();
		}
	}

	public List<LdapNode> searchBy(LdapName dn, CustomSearchFilter filter) {

		NamingEnumeration<SearchResult> result = this.searchSubtree(dn, "(&(ou=peaple)(ouCode=P*T))");

		return Collections.list(result)
				.stream()
				.map(this::toNode)
				.collect(Collectors.toList());
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

	public LdapAttribute toAttr(Attributes attrs) {

		List<? extends Attribute> attrsList = Collections.list(attrs.getAll());

		LdapAttribute result = new LdapAttribute(attrsList.size());

		try {
			for (Attribute attr : attrsList) {

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
			return result;
		} catch (NamingException e) {
			if (isTimeOutError(e)) {
				this.reconnect = true;
				return this.toAttr(attrs);
			}
			throw new LogicError(e);
		} finally {
			this.disconnect();
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
