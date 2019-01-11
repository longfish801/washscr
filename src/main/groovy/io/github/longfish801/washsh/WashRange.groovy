/*
 * WashRange.groovy
 *
 * Copyright (C) io.github.longfish801 All Rights Reserved.
 */
package io.github.longfish801.washsh;

import groovy.transform.InheritConstructors;
import groovy.util.logging.Slf4j;
import io.github.longfish801.shared.ArgmentChecker;
import io.github.longfish801.tpac.element.TeaHandle;
import io.github.longfish801.tpac.element.TpacRefer;
import io.github.longfish801.tpac.element.TpacText;
import io.github.longfish801.tpac.parser.TeaMakerMakeException;
import java.util.regex.Pattern;

/**
 * washスクリプトの rangeハンドルならびにその下位ハンドルです。
 * @version 1.0.00 2018/09/17
 * @author io.github.longfish801
 */
@Slf4j('LOG')
class WashRange implements TeaHandle {
	/** GroovyShell */
	static GroovyShell shell = new GroovyShell(WashRange.class.classLoader);
	
	/**
	 * このハンドラの妥当性を検証します。
	 */
	@Override
	void validate(){
		if (!name.empty) throw new TeaMakerMakeException("rangeには名前を指定できません。key=${key}");
	}
	
	/**
	 * テキスト範囲の下位要素を作成します。
	 * @param lines 行リスト
	 * @param node テキスト範囲のノード
	 * @param bgnIdx 走査開始位置
	 * @param endIdx 走査終了位置
	 */
	void tagging(List lines, TextRange.Node node, int bgnIdx, int endIdx){
		int idx = bgnIdx;
		int preIdx;
		node << node.newInstanceLeaf();
		while (idx <= endIdx){
			preIdx = idx;
			if (lowers.values().any { (idx = it.tagging(lines, node, idx, endIdx)) > preIdx }){
				node << node.newInstanceLeaf();
			} else {
				node.lowers.last() << lines[idx ++];
			}
		}
		if (node.lowers.last().lines.empty) node.lowers.removeLast();
	}
	
	/**
	 * maskハンドルに対応するインスタンスを新規作成します。
	 * @return maskハンドルに対応するインスタンス
	 */
	WashMask newInstanceMask(){
		return new WashMask();
	}
	
	/**
	 * dividedハンドルに対応するインスタンスを新規作成します。
	 * @return dividedハンドルに対応するインスタンス
	 */
	WashDivided newInstanceDivided(){
		return new WashDivided();
	}
	
	/**
	 * enclosedハンドルに対応するインスタンスを新規作成します。
	 * @return enclosedハンドルに対応するインスタンス
	 */
	WashEnclosed newInstanceEnclosed(){
		return new WashEnclosed();
	}
	
	/**
	 * treeハンドルに対応するインスタンスを新規作成します。
	 * @return treeハンドルに対応するインスタンス
	 */
	WashTree newInstanceTree(){
		return new WashTree();
	}
	
	/**
	 * rangeハンドルの下位ハンドルの操作の特性です。
	 */
	trait RangeOperator {
		/**
		 * テキスト範囲の下位要素を作成します。
		 * @param lines 行リスト
		 * @param node テキスト範囲のノード
		 * @param idx 走査位置
		 * @param endIdx 走査終了位置
		 * @return 新しい走査位置
		 */
		abstract int tagging(List lines, TextRange.Node node, int idx, int endIdx);
	}
	
	/**
	 * maskハンドルです。
	 */
	class WashMask implements TeaHandle, RangeOperator {
		/** マスキング箇所の開始か否か判定するクロージャ */
		Closure isMaskBgn;
		/** マスキング箇所の終了か否か判定するクロージャ */
		Closure isMaskEnd;
		/** マスキング範囲内か否か */
		boolean inMask = false;
		/** マスキング開始行 */
		String bgnLine;
		
		/**
		 * このハンドラの妥当性を検証します。
		 */
		@Override
		void validate(){
			if (!name.empty) throw new TeaMakerMakeException("maskには名前を指定できません。key=${key}");
			if (upper.lowers.keySet().toArray()[0] != key) throw new TeaMakerMakeException("maskハンドルは rangeハンドル内の先頭にひとつだけ記述してください。key=${key}");
			if (map.bgn == null) throw new TeaMakerMakeException("bgnは必須です。key=${key}");
			if (map.bgn != null && (!(map.bgn instanceof String) && !(map.bgn instanceof Pattern))) {
				throw new TeaMakerMakeException("bgnは文字列あるいは正規表現を指定してください。key=${key}");
			}
			if (map.end != null && (!(map.end instanceof String) && !(map.end instanceof Pattern))) {
				throw new TeaMakerMakeException("endは文字列あるいは正規表現を指定してください。key=${key}");
			}
			isMaskBgn = { String line ->
				return ((map.bgn instanceof String && line == map.bgn)
				     || (map.bgn instanceof Pattern && line ==~ map.bgn));
			}
			isMaskEnd = { String line, String bgnLine ->
				return ((map.end == null && line == bgnLine)
				     || (map.end instanceof String && line == map.end)
				     || (map.end instanceof Pattern && line ==~ map.end));
			}
		}
		
		/**
		 * テキスト範囲の下位要素を作成します。
		 * @param lines 行リスト
		 * @param node テキスト範囲のノード
		 * @param idx 走査位置
		 * @param endIdx 走査終了位置
		 * @return 新しい走査位置
		 */
		int tagging(List lines, TextRange.Node node, int idx, int endIdx){
			if (!isMaskBgn.call(lines[idx])) return idx;
			int bgnIdx = idx;
			TextRange.Node maskedNode = node.newInstanceNode('masked');
			node << maskedNode;
			maskedNode << node.newInstanceLeaf();
			while (true){
				idx ++;
				if (idx > endIdx) throw new WashRangeParseException("マスキング箇所の開始に対し、終了が記述されていません。key=${key} 行番号=${bgnIdx + 1} 開始行=${lines[bgnIdx]}");
				if (isMaskEnd.call(lines[idx], lines[bgnIdx])) break;
				maskedNode.lowers.last() << lines[idx];
			}
			return idx + 1;
		}
	}
	
	/**
	 * dividedハンドルです。
	 */
	class WashDivided implements TeaHandle, RangeOperator {
		/** 範囲の区切りか否か判定するクロージャ */
		Closure isRangeDiv;
		
		/**
		 * このハンドラの妥当性を検証します。
		 */
		@Override
		void validate(){
			if (name.empty) throw new TeaMakerMakeException("ハンドルに名前が定義されていません。key=${key}");
			if (map.div == null) throw new TeaMakerMakeException("divが定義されていません。key=${key}");
			if (map.div != null && (!(map.div instanceof String) && !(map.div instanceof Pattern))) {
				throw new TeaMakerMakeException("divは文字列あるいは正規表現を指定してください。key=${key}");
			}
			isRangeDiv = { String line ->
				return ((map.div instanceof String && line == map.div)
				     || (map.div instanceof Pattern && line ==~ map.div));
			}
		}
		
		/**
		 * テキスト範囲の下位要素を作成します。
		 * @param lines 行リスト
		 * @param node テキスト範囲のノード
		 * @param idx 走査位置
		 * @param endIdx 走査終了位置
		 * @return 新しい走査位置
		 */
		int tagging(List lines, TextRange.Node node, int idx, int endIdx){
			if (!isRangeDiv.call(lines[idx])) return idx;
			int bgnIdx = idx;
			while (true){
				idx ++;
				if (idx > endIdx) throw new WashRangeParseException("範囲の開始に対し、終了が記述されていません。key=${key} 行番号=${bgnIdx + 1} 開始行=${lines[bgnIdx]}");
				if (isRangeDiv.call(lines[idx])) break;
			}
			TextRange.Node dividedNode = node.newInstanceNode(name);
			node << dividedNode;
			dividedNode.labels.bgn = lines[bgnIdx];
			dividedNode.labels.end = lines[idx];
			upper.tagging(lines, dividedNode, bgnIdx + 1, idx - 1);
			return idx + 1;
		}
	}
	
	/**
	 * enclosedハンドルです。
	 */
	class WashEnclosed implements TeaHandle, RangeOperator {
		/** 範囲開始行 */
		String bgnLine;
		/** 範囲の開始か否か判定するクロージャ */
		Closure isRangeBgn;
		/** 範囲の終了か否か判定するクロージャ */
		Closure isRangeEnd;
		
		/**
		 * このハンドラの妥当性を検証します。
		 */
		@Override
		void validate(){
			if (name.empty) throw new TeaMakerMakeException("ハンドルに名前が定義されていません。key=${key}");
			if (map.bgn == null) throw new TeaMakerMakeException("bgnが定義されていません。key=${key}");
			if (map.bgn != null && (!(map.bgn instanceof String) && !(map.bgn instanceof Pattern))) {
				throw new TeaMakerMakeException("bgnは文字列あるいは正規表現を指定してください。key=${key}");
			}
			isRangeBgn = { String line ->
				return ((map.bgn instanceof String && line == map.bgn)
				     || (map.bgn instanceof Pattern && line ==~ map.bgn));
			}
			if (map.end == null) throw new TeaMakerMakeException("endが定義されていません。key=${key}");
			if (map.end != null && (!(map.end instanceof String) && !(map.end instanceof Pattern))) {
				throw new TeaMakerMakeException("endは文字列あるいは正規表現を指定してください。key=${key}");
			}
			isRangeEnd = { String line ->
				return ((map.end instanceof String && line == map.end)
				     || (map.end instanceof Pattern && line ==~ map.end));
			}
		}
		
		/**
		 * テキスト範囲の下位要素を作成します。
		 * @param lines 行リスト
		 * @param node テキスト範囲のノード
		 * @param idx 走査位置
		 * @param endIdx 走査終了位置
		 * @return 新しい走査位置
		 */
		int tagging(List lines, TextRange.Node node, int idx, int endIdx){
			if (!isRangeBgn.call(lines[idx])) return idx;
			int bgnIdx = idx;
			List stacks = [];
			while (true){
				idx ++;
				if (idx > endIdx) throw new WashRangeParseException("範囲の開始に対し、終了が記述されていません。key=${key} 行番号=${bgnIdx + 1} 開始行=${lines[bgnIdx]}");
				if (isRangeBgn.call(lines[idx])){
					stacks << idx;
				} else if (isRangeEnd.call(lines[idx])){
					if (stacks.size() == 0) break;
					stacks.pop();
				}
			}
			TextRange.Node enclosedNode = node.newInstanceNode(name);
			node << enclosedNode;
			enclosedNode.labels.bgn = lines[bgnIdx];
			enclosedNode.labels.end = lines[idx];
			upper.tagging(lines, enclosedNode, bgnIdx + 1, idx - 1);
			return idx + 1;
		}
	}
	
	/**
	 * treeハンドルです。
	 */
	class WashTree implements TeaHandle, RangeOperator {
		/** levelクロージャ */
		Closure levelCl;
		/** escapeクロージャ */
		Closure escapeCl;
		
		/**
		 * このハンドラの妥当性を検証します。
		 */
		@Override
		void validate(){
			if (name.empty) throw new TeaMakerMakeException("ハンドルに名前が定義されていません。key=${key}");
			if (map.level == null || (!(map.level instanceof TpacRefer) && !(map.level instanceof TpacText))){
				throw new TeaMakerMakeException("levelは参照あるいはテキストで定義してください。key=${key}");
			}
			levelCl = (map.level instanceof TpacText)? shell.evaluate(map.level.toString(), "${key}_level.groovy") : { String line -> map.level.refer().call(line) };
			if (map.escape == null || (!(map.escape instanceof TpacRefer) && !(map.escape instanceof TpacText))){
				throw new TeaMakerMakeException("escapeは参照あるいはテキストで定義してください。key=${key}");
			}
			escapeCl = (map.escape instanceof TpacText)? shell.evaluate(map.escape.toString(), "${key}_escape.groovy") : { int idx, int endIdx, int level, List lines -> map.escape.refer().call(idx, endIdx, level, lines) };
		}
		
		/**
		 * テキスト範囲の下位要素を作成します。
		 * @param lines 行リスト
		 * @param node テキスト範囲のノード
		 * @param idx 走査位置
		 * @param endIdx 走査終了位置
		 * @return 新しい走査位置
		 */
		int tagging(List lines, TextRange.Node node, int idx, int endIdx){
			int level = levelCl.call(lines[idx]);
			if (level < 0) return idx;
			if (node.upper != null && node.upper.name == name && node.upper.labels.level >= level) return idx;
			TextRange.Node treeNode = node.newInstanceNode(name);
			treeNode.labels.first = lines[idx];
			treeNode.labels.level = level;
			node << treeNode;
			int bgnIdx = idx;
			int elemBgnIdx = idx;
			int elemNo = 0;
			String firstLine;
			Closure createElem = { int elemEndIdx ->
				TextRange.Node elemNode = node.newInstanceNode("${name}#elem");
				treeNode << elemNode;
				elemNode.labels.first = firstLine;
				elemNode.labels.number = ++ elemNo;
				upper.tagging(lines, elemNode, elemBgnIdx, elemEndIdx);
			}
			while (true){
				int elemLevel = levelCl.call(lines[idx]);
				if (elemLevel == level) firstLine = lines[idx];
				if (idx > bgnIdx && elemLevel == level){
					createElem.call(idx - 1);
					elemBgnIdx = idx;
				}
				boolean isEscape = escapeCl.call(idx, endIdx, level, lines);
				if (isEscape){
					createElem.call(idx);
					break;
				}
				idx ++;
				if (idx > endIdx) throw new WashRangeParseException("範囲の開始に対し、終了が記述されていません。key=${key} 行番号=${bgnIdx + 1} 開始行=${lines[bgnIdx]}");
			}
			treeNode.labels.size = elemNo;
			return idx + 1;
		}
	}
	
	/**
	 * washshスクリプトによる分類の失敗を表す例外です。
	 */
	@InheritConstructors
	class WashRangeParseException extends Exception { }
}
