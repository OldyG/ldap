/**
 * FileName : IsParentPredicate.java
 * Created  : 2021. 3. 17.
 * Author   : hokkk
 * Summary  :
 * Copyright (C) 2021 Goldy Project Inc. All rights reserved.
 * 이 문서의 모든 저작권 및 지적 재산권은 Goldy Project에게 있습니다.
 * 이 문서의 어떠한 부분도 허가 없이 복제 또는 수정 하거나, 전송할 수 없습니다.
 */
package com.naonsoft.example.tools.tree;

@FunctionalInterface
public interface IsParentPredicate<T> {

	boolean isParent(T parentTarget, T current);
}
