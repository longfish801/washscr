/*
 * TagLines.groovy
 *
 * Copyright (C) io.github.longfish801 All Rights Reserved.
 */
package io.github.longfish801.washsh;

import groovy.util.logging.Slf4j;

/**
 * タグ付き行リストです。
 * @version 1.0.00 2018/09/27
 * @author io.github.longfish801
 */
@Slf4j('LOG')
class TagLines {
	/** 各行 */
	List<String> lines = [];
	/** 各行の種類 */
	List<String> kinds = [];
	/** 各行のタグ */
	List<List<String>> tags = [];
	
	/**
	 * 行文字列とその種類を追加します。<br/>
	 * 種類が nullの場合、デフォルト値として "plain"を格納します。
	 * @param line 行
	 * @param kind 種類
	 */
	void append(String line, String kind){
		lines << line;
		kinds << (kind ?: 'plain');
	}
	
	/**
	 * 指定した行番号の位置にタグを追加します。
	 * @param idx 行番号
	 * @param tag タグ
	 */
	void appendTag(int idx, String tag){
		if (tags[idx] == null) tags[idx] = [];
		if (tag != null) tags[idx] << tag;
	}
}
