/*
 * Conveyer.groovy
 *
 * Copyright (C) io.github.longfish801 All Rights Reserved.
 */
package io.github.longfish801.washsh.conveyer;

import groovy.util.logging.Slf4j;

/**
 * キューに要素を格納するためのクラスです。
 * @version 1.0.00 2017/07/27
 * @author io.github.longfish801
 */
@Slf4j('LOG')
class Product<E> {
	/** 要素 */
	E elem;
	/** 処理対象とするか否か */
	boolean target = true;
	
	/**
	 * コンストラクタ。
	 * @param elem 要素
	 */
	Product(E elem){
		this.elem = elem;
	}
	
	/**
	 * コンストラクタ。
	 * @param elem 要素
	 * @param target 処理対象とするか否か
	 */
	Product(E elem, boolean target){
		this.elem = elem;
		this.target = target;
	}
	
	/**
	 * 要素を格納して自インスタンスを返します。
	 * @param elem 要素
	 * @return 自インスタンス
	 */
	Product store(E elem){
		this.elem = elem;
		return this;
	}
}
