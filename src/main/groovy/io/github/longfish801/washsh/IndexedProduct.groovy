/*
 * IndexedProduct.groovy
 *
 * Copyright (C) io.github.longfish801 All Rights Reserved.
 */
package io.github.longfish801.washsh;

import groovy.util.logging.Slf4j;
import io.github.longfish801.washsh.conveyer.Product;

/**
 * 位置情報を保持する要素のクラスです。
 * @version 1.0.00 2017/08/01
 * @author io.github.longfish801
 */
@Slf4j('LOG')
class IndexedProduct<E> extends Product {
	/** 位置 */
	int index;
	/** 最後の要素か否か */
	boolean isLast = false;
	
	/**
	 * コンストラクタ。
	 * @param elem 要素
	 * @param target 処理対象とするか否か
	 * @param index 位置
	 * @param isLast 最後の要素か否か
	 */
	IndexedProduct(E elem, boolean target, int index, boolean isLast){
		super(elem, target);
		this.index = index;
		this.isLast = isLast;
	}
}
