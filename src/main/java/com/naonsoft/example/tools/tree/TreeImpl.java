/**
 * FileName : TreeImpl.java
 * Created  : 2020. 3. 28.
 * Author   : jeonghyun.kum
 * Summary  :
 * Copyright (C) 2021 Goldy Project Inc. All rights reserved.
 *
 * 이 문서의 모든 저작권 및 지적 재산권은 Goldy Project에게 있습니다.
 * 이 문서의 어떠한 부분도 허가 없이 복제 또는 수정 하거나, 전송할 수 없습니다.
 */
package com.naonsoft.example.tools.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 트리 구현체
 * 
 * @param <T>
 *            입력 객체 타입
 */
public class TreeImpl<T> implements Tree<T> {

	/** 부모 객체 */
	private Tree<T> parent;

	/** 이 노드의 데이터 */
	private final T data;

	/** 자식 데이터 목록 */
	private final List<Tree<T>> childs;

	/**
	 * TreeImpl 클래스의 새 인스턴스를 초기화 합니다.
	 * 
	 * @param parent
	 *            부모 데이터 Root인 경우 null 입력
	 * @param data
	 *            현 데이터<br>
	 *            null 입력 불가
	 */
	public TreeImpl(Tree<T> parent, T data) {

		this(parent, data, new LinkedList<>());

	}

	/**
	 * TreeImpl 클래스의 새 인스턴스를 초기화 합니다.
	 * 
	 * @param parent
	 *            부모 데이터 Root인 경우 null 입력
	 * @param data
	 *            현 데이터<br>
	 *            null 입력 불가
	 * @param childs
	 *            자식 데이터
	 */
	public TreeImpl(Tree<T> parent, T data, List<Tree<T>> childs) {

		super();
		this.parent = parent;
		this.data = data;
		this.childs = new ArrayList<>(childs);
	}

	public static <T> void link(TreeImpl<T> parent, TreeImpl<T> child) {

		child.parent = parent;
		parent.childs.add(child);
	}

	public static <T> List<Tree<T>> toTree(Collection<T> datas, IsParentPredicate<T> predicate) {

		List<TreeImpl<T>> trees = datas.stream()
				.map(t -> new TreeImpl<>(null, t))
				.collect(Collectors.toList());

		for (TreeImpl<T> tree : trees) {

			for (TreeImpl<T> tree2 : trees) {
				if (predicate.isParent(tree.getData(), tree2.getData())) {
					link(tree, tree2);
				}
			}
		}
		return trees.stream()
				.filter(Tree::isRoot)
				.collect(Collectors.toList());
	}

	@Override
	public Tree<T> addChild(T childData) {

		TreeImpl<T> child = new TreeImpl<>(this, childData);
		this.childs.add(child);
		return child;
	}

	@Override
	public Optional<Tree<T>> getChild(int index) {

		if (this.childs.size() < index) {
			return Optional.empty();
		}
		return Optional.of(this.childs.get(index));
	}

	@Override
	public int getChildCount() {

		return this.childs.size();
	}

	@Override
	public List<T> getChildDatas() {

		return this.childs
				.stream()
				.map(Tree::getData)
				.collect(Collectors.toList());
	}

	@Override
	public List<Tree<T>> getChilds() {

		return new ArrayList<>(this.childs);
	}

	/**
	 * data를 반환합니다.
	 * 
	 * @return data
	 */
	@Override
	public T getData() {

		return this.data;
	}

	@Override
	public int getIndexOfChild(T child) {

		return this.getChildDatas()
				.indexOf(child);
	}

	@JsonIgnore
	@Override
	public Optional<Tree<T>> getParent() {

		return Optional.ofNullable(this.parent);
	}

	@JsonIgnore
	@Override
	public Tree<T> getRoot() {

		Optional<Tree<T>> p = this.getParent();
		if (p.isPresent() == false) {
			return this;
		}
		return p.get().getRoot();
	}

	@Override
	public boolean isLeaf() {

		return this.childs.isEmpty();
	}

	@Override
	public boolean isRoot() {

		return this.parent == null;
	}

	@Override
	public Iterator<T> iterator() {

		List<T> list = this.toList();

		return new Iterator<T>() {

			int cursor = 0;

			@Override
			public boolean hasNext() {

				return this.cursor != list.size();
			}

			@Override
			public T next() {

				T t = list.get(this.cursor);
				this.cursor++;
				return t;
			}
		};
	}

	private void recursiveString(StringBuilder builder, int depth, List<Tree<T>> childs2) {

		StringBuilder builder2 = new StringBuilder();
		for (int i = 0; i < depth; i++) {
			builder2.append('\t');
		}
		builder2.append("- ");

		for (Tree<T> tree : childs2) {
			builder.append(builder2).append(tree.getData()).append("\r\n");
			this.recursiveString(builder, depth + 1, tree.getChilds());
		}
	}

	@Override
	public long size() {

		long result = 1;

		for (Tree<T> tree : this.childs) {
			result += tree.size();
		}
		return result;
	}

	@Override
	public void sort(Comparator<T> comparator) {

		this.childs.sort((o1, o2) -> {
			return comparator.compare(o1.getData(), o2.getData());
		});
	}

	@Override
	public void sortRecursive(Comparator<T> comparator) {

		this.childs.sort((o1, o2) -> {
			return comparator.compare(o1.getData(), o2.getData());
		});

		for (Tree<T> tree : this.childs) {
			tree.sortRecursive(comparator);
		}
	}

	@Override
	public List<T> toList() {

		List<T> result = new ArrayList<>();
		result.add(this.data);
		for (Tree<T> child : this.childs) {
			result.addAll(child.toList());
		}
		return result;
	}

	@Override
	public String toString() {

		StringBuilder builder = new StringBuilder();
		builder.append(this.data).append("\r\n");

		this.recursiveString(builder, 1, this.childs);

		return builder.toString();
	}
}
