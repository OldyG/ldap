/**
 * FileName : Tree.java
 * Created  : 2020. 3. 28.
 * Author   : jeonghyun.kum
 * Summary  :
 * Copyright (C) 2021 Goldy Project Inc. All rights reserved.
 *
 * 이 문서의 모든 저작권 및 지적 재산권은 Goldy Project에게 있습니다.
 * 이 문서의 어떠한 부분도 허가 없이 복제 또는 수정 하거나, 전송할 수 없습니다.
 */
package com.naonsoft.example.tools.tree;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 트리
 * 
 * @param <T>
 *            관리 데이터
 */
public interface Tree<T> extends Iterable<T> {

	Tree<T> addChild(T data);

	Optional<Tree<T>> getChild(int index);

	@JsonIgnore
	int getChildCount();

	@JsonIgnore
	List<T> getChildDatas();

	List<Tree<T>> getChilds();

	T getData();

	int getIndexOfChild(T child);

	Optional<Tree<T>> getParent();

	Tree<T> getRoot();

	boolean isLeaf();

	boolean isRoot();

	long size();

	void sort(Comparator<T> comparator);

	void sortRecursive(Comparator<T> comparator);

	List<T> toList();

}
