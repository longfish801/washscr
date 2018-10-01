/*
 * TagText.groovy
 *
 * Copyright (C) io.github.longfish801 All Rights Reserved.
 */
package io.github.longfish801.washsh;

import groovy.util.logging.Slf4j;

/**
 * タグ付きテキストです。
 * @version 1.0.00 2018/09/17
 * @author io.github.longfish801
 */
@Slf4j('LOG')
class TagText {
	/** タグ一覧 */
	List<String> tags;
	/** テキスト */
	String text = '';
	
	/**
	 * コンストラクタ。
	 * @param tags タグ一覧
	 */
	TagText(List<String> tags){
		this.tags = tags;
	}
}
