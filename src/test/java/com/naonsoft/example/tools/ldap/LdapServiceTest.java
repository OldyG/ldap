package com.naonsoft.example.tools.ldap;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.naming.NamingEnumeration;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchResult;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.StopWatch;
import org.springframework.util.StopWatch.TaskInfo;

import com.naonsoft.example.tools.tree.Tree;

class LdapServiceTest {

	private static String LDAP_URL;

	private LdapService target;

	@BeforeAll
	static void setup2() throws IOException {

		URL resource = LdapServiceTest.class.getResource("ldapUrl.txt");
		File file = new File(resource.getFile());
		LDAP_URL = Files.readAllLines(file.toPath()).stream()
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("ldapUrl을 찾을 수 없음"));

	}

	@BeforeEach
	void setup() {

		LdapConnection connection = new LdapConnection(LDAP_URL);
		this.target = new LdapService(connection);
	}

	@Test
	void test1() {

		LdapNode root = this.target.getRoots().get(0);
		List<LdapNode> childs = this.target.getChilds("o=government of korea,c=kr");
		for (LdapNode ldapNode : childs) {
			System.out.println(ldapNode);
		}
	}

	@Test
	void test3() {

		List<LdapNode> childs = this.target.getChilds("ou=people,ou=BNK캐피탈,o=private of korea,c=kr");

		List<String> collect = childs.stream()
				.map(LdapNode::getAttributes)
				.flatMap(e -> e.keySet().stream())
				.distinct()
				.sorted(StringUtils::compareIgnoreCase)
				.collect(Collectors.toList());

		for (String string : collect) {
			System.out.println(string);
		}

		System.out.println(childs);
	}

	@Test
	void test4() {

		NamingEnumeration<SearchResult> searchSubtree = this.target.searchSubtree("ou=BNK캐피탈,o=private of korea,c=kr",
				"(objectClass=user)");

		List<List<String>> collect = Collections.list(searchSubtree)
				.stream()
				.map(this.target::toNode)
				.map(LdapNode::getAttributes)
				.map(e -> e.get("cn"))
				.collect(Collectors.toList());

		for (List<String> list : collect) {
			System.out.println(list);
		}
	}

	/**
	 * (!) 데이터가 너무 많아 40분정도 수행후 50,000건 정도 수집함
	 */
	@Test
	void testAllTree() {

		StopWatch sw = new StopWatch("allTree");
		sw.start();
		this.target.allTree();
		sw.stop();
		System.out.println(sw.prettyPrint());
	}

	@Test
	void testGetDir() {

		DirContext dir = this.target.getDir("ou=4차산업혁명위원회,o=government of korea,c=kr");

		System.out.println(dir);
	}

	@Test
	void testGetRoots() {

		List<LdapNode> roots = this.target.getRoots();

		for (LdapNode root : roots) {
			System.out.println(root);
		}
	}

	@Test
	void testSearch() {

		NamingEnumeration<SearchResult> search = this.target.searchOneLevel("c=kr", "(objectClass=*)");

		int cnt = 0;
		while (search.hasMoreElements()) {
			System.out.println(search.nextElement().getNameInNamespace());
			cnt++;
		}
		System.out.println(cnt);
	}

	@Test
	void testTree() {

		StopWatch sw = new StopWatch("tree");
		sw.start("treeSlow");
		Tree<LdapNode> slow = this.target.treeSlow("ou=현대캐피탈,o=private of korea,c=kr");
		sw.stop();

		sw.start("treeQuick");
		Tree<LdapNode> quick = this.target.tree("ou=현대캐피탈,o=private of korea,c=kr");
		sw.stop();
		TaskInfo[] taskInfo = sw.getTaskInfo();
		for (TaskInfo taskInfo2 : taskInfo) {
			System.out.println(taskInfo2.getTaskName() + " " + taskInfo2.getTimeSeconds());
		}
	}

	@Test
	void testTreeQuick() {

		StopWatch sw = new StopWatch("tree");

		sw.start("treeQuick");
		Tree<LdapNode> quick = this.target.tree("ou=현대캐피탈,o=private of korea,c=kr");
		sw.stop();
		TaskInfo[] taskInfo = sw.getTaskInfo();
		for (TaskInfo taskInfo2 : taskInfo) {
			System.out.println(taskInfo2.getTaskName() + " " + taskInfo2.getTimeSeconds());
		}

		quick.toList()
				.stream()
				.map(LdapNode::getAttributes)
				.flatMap(e -> e.keySet().stream())
				.distinct()
				.sorted()
				.forEach(System.out::println);

		System.out.println(quick.size());
	}

}
