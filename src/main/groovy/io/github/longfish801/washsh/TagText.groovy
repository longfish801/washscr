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
	/** 上位要素 */
	TagText upper;
	
	/**
	 * Nodeインスタンスを新規作成します。
	 * @param tag タグ
	 * @return Nodeインスタンス
	 */
	Node newInstanceNode(String tag){
		Node node = new Node();
		node.tag = tag;
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
	 * タグ付きテキストのノードです。
	 */
	class Node extends TagText {
		/** タグ */
		String tag;
		/** 開始行 */
		String bgn;
		/** 終端行 */
		String end;
		/** 下位要素 */
		List<TagText> lowers = [];
		
		/**
		 * 下位テキストを追加します。
		 * @param lower 下位ハンドラ
		 * @return 自インスタンス
		 */
		Node leftShift(TagText lower){
			lowers << lower;
			lower.upper = this;
			return this;
		}
		
		/**
		 * 出力します。
		 * @param writer Writer
		 */
		void write(Writer writer){
			if (bgn != null) writer << bgn + System.lineSeparator();
			lowers.each { it.write(writer) }
			if (end != null) writer << end + System.lineSeparator();
		}
		
		/**
		 * タグ一覧を返します。
		 * @return タグ一覧
		 */
		List<String> getTags(){
			if (upper == null) return (tag.empty)? [] : [ tag ];
			return (tag.empty)? upper.tags : [ *(upper.tags), tag ];
		}
		
		/**
		 * 文字列表現を返します。
		 * @return 文字列表現
		 */
		String toString(){
			String lowersStr = lowers.collect { it.toString() }.join("\n    - ");
			return "node(tag=${tag} bgn=${bgn} end=${end} lowers=${lowersStr})";
		}
	}
	
	/**
	 * タグ付きテキストのテキストです。
	 */
	class Leaf extends TagText {
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
		 * タグ一覧を返します。
		 * @return タグ一覧
		 */
		List<String> getTags(){
			return upper.tags;
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
