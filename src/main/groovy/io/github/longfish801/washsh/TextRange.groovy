/*
 * TextRange.groovy
 *
 * Copyright (C) io.github.longfish801 All Rights Reserved.
 */
package io.github.longfish801.washsh;

import groovy.util.logging.Slf4j;

/**
 * テキスト範囲です。
 * @version 1.0.00 2018/09/17
 * @author io.github.longfish801
 */
@Slf4j('LOG')
class TextRange {
	/** 上位要素 */
	TextRange upper;
	
	/**
	 * Nodeインスタンスを新規作成します。
	 * @param name 範囲名
	 * @return Nodeインスタンス
	 */
	Node newInstanceNode(String name){
		Node node = new Node();
		node.name = name;
		return node;
	}
	
	/**
	 * Leafインスタンスを新規作成します。
	 * @return Leafインスタンス
	 */
	Leaf newInstanceLeaf(){
		return new Leaf();
	}
	
	/**
	 * テキスト範囲のノードです。
	 */
	class Node extends TextRange {
		/** 範囲名 */
		String name;
		/** ラベル */
		Map<String, String> labels = [:];
		/** 下位要素 */
		List<TextRange> lowers = [];
		
		/**
		 * 下位テキストを追加します。
		 * @param lower 下位ハンドラ
		 * @return 自インスタンス
		 */
		Node leftShift(TextRange lower){
			lowers << lower;
			lower.upper = this;
			return this;
		}
		
		/**
		 * 出力します。
		 * @param writer Writer
		 */
		void write(Writer writer){
			if (labels.bgn != null) writer << labels.bgn + System.lineSeparator();
			lowers.each { it.write(writer) }
			if (labels.end != null) writer << labels.end + System.lineSeparator();
		}
		
		/**
		 * 範囲名階層を返します。
		 * @return 範囲名階層
		 */
		List<String> getTags(){
			if (upper == null) return (name.empty)? [] : [ name ];
			return (name.empty)? upper.names : [ *(upper.names), name ];
		}
		
		/**
		 * 文字列表現を返します。
		 * @return 文字列表現
		 */
		String toString(){
			String lowersStr = lowers.collect { it.toString() }.join("\n    - ");
			return "node(name=${name} bgn=${labels.bgn} end=${labels.end} lowers=${lowersStr})";
		}
	}
	
	/**
	 * テキスト範囲のテキストです。
	 */
	class Leaf extends TextRange {
		/** 行リスト */
		List lines = [];
		
		/**
		 * 行を追加します。</br>
		 * 行末に改行コードとして「\n」を付与します。
		 * @param line 行
		 * @return 自インスタンス
		 */
		Leaf leftShift(String line){
			if (line != null) lines << line;
			return this;
		}
		
		/**
		 * 出力します。
		 * @param writer Writer
		 */
		void write(Writer writer){
			if (lines == null) return;
			lines.each { writer << it + System.lineSeparator() }
		}
		
		/**
		 * 範囲名階層を返します。
		 * @return 範囲名階層
		 */
		List<String> getTags(){
			return upper.names;
		}
		
		/**
		 * 文字列表現を返します。
		 * @return 文字列表現
		 */
		String toString(){
			return lines.toString();
		}
	}
}
