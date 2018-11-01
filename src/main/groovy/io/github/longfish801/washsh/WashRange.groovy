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
	 * 指定された行の種類を判定します。
	 * @param lines 行リスト
	 * @return 種類付き行リスト
	 */
	List kindof(List lines){
		List newLines = [];
		Iterator iterator = lines.iterator();
		while(iterator.hasNext()){
			Map map = iterator.next();
			if (lowers.values().every { !it.kindof(map, newLines) }) newLines << map;
			iterator.remove();
		}
		return newLines;
	}
	
	/**
	 * タグ付けの準備をします。
	 * @param lines 種類付き行リスト
	 */
	void taggingSetup(List lines){
		lowers.values().each { it.taggingSetup(lines) };
	}
	
	/**
	 * 行毎にタグ付けをします。
	 * @param lines 種類付き行リスト
	 * @param node タグ付きテキストのノード
	 * @param bgnIdx 走査開始位置
	 * @param endIdx 走査終了位置
	 */
	void tagging(List lines, TagText.Node node, int bgnIdx, int endIdx){
		int idx = bgnIdx;
		int preIdx;
		node << node.newInstanceLeaf();
		while (idx <= endIdx){
			preIdx = idx;
			if (lowers.values().any { (idx = it.tagging(lines, node, idx, endIdx)) > preIdx }){
				node << node.newInstanceLeaf();
			} else {
				node.lowers.last() << lines[idx ++].line;
			}
		}
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
		 * 指定された行の種類を判定します。
		 * @param map 行マップ
		 * @param newLines 種類付き行リスト
		 * @return 種類の判定をしたか否か
		 */
		abstract boolean kindof(Map map, List newLines);
		
		/**
		 * タグ付けの準備をします。
		 * @param lines 種類付き行リスト
		 */
		void taggingSetup(List lines){
			// なにもしません
		}
		
		/**
		 * 行毎にタグ付けをします。
		 * @param lines 種類付き行リスト
		 * @param node タグ付きテキストのノード
		 * @param idx 走査位置
		 * @param endIdx 走査終了位置
		 * @return 新しい走査位置
		 */
		abstract int tagging(List lines, TagText.Node node, int idx, int endIdx);
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
		 * 指定された行の種類を判定します。
		 * @param map 行マップ
		 * @param newLines 種類付き行リスト
		 * @return 種類の判定をしたか否か
		 */
		boolean kindof(Map map, List newLines){
			if (inMask){
				if (isMaskEnd.call(map.line, bgnLine)){
					inMask = false;
					bgnLine = null;
					map.kind = 'masked#end';
					newLines << map;
					return true;
				} else {
					newLines << map;
				}
			} else {
				if (isMaskBgn.call(map.line)){
					inMask = true;
					bgnLine = map.line;
					map.kind = 'masked#bgn';
					newLines << map;
				}
			}
			return (map.kind == 'masked#end')? true : inMask;
		}
		
		/**
		 * 行毎にタグ付けをします。
		 * @param lines 種類付き行リスト
		 * @param node タグ付きテキストのノード
		 * @param idx 走査位置
		 * @param endIdx 走査終了位置
		 * @return 新しい走査位置
		 */
		int tagging(List lines, TagText.Node node, int idx, int endIdx){
			if (lines[idx].kind != 'masked#bgn') return idx;
			int bgnIdx = idx;
			TagText.Node maskedNode = node.newInstanceNode('masked');
			node << maskedNode;
			maskedNode << node.newInstanceLeaf();
			idx ++;
			while (lines[idx].kind != 'masked#end'){
				maskedNode.lowers.last() << lines[idx ++].line;
				if (idx > endIdx) throw new WashRangeParseException("マスキング箇所の開始に対し、終了が記述されていません。key=${key} 行番号=${lines[bgnIdx].lineNo} 開始行=${lines[bgnIdx].line}");
			}
			return ++ idx;
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
		 * 指定された行の種類を判定します。
		 * @param map 行マップ
		 * @param newLines 種類付き行リスト
		 * @return 種類の判定をしたか否か
		 */
		boolean kindof(Map map, List newLines){
			if (!isRangeDiv.call(map.line)) return false;
			map.kind = "${name}#div";
			newLines << map;
			return true;
		}
		
		/**
		 * 行毎にタグ付けをします。
		 * @param lines 種類付き行リスト
		 * @param node タグ付きテキストのノード
		 * @param idx 走査位置
		 * @param endIdx 走査終了位置
		 * @return 新しい走査位置
		 */
		int tagging(List lines, TagText.Node node, int idx, int endIdx){
			if (lines[idx].kind != "${name}#div") return idx;
			int bgnIdx = idx;
			TagText.Node dividedNode = node.newInstanceNode(name);
			node << dividedNode;
			dividedNode.bgn = lines[idx ++].line;
			while (lines[idx].kind != "${name}#div"){
				if (idx > endIdx) throw new WashRangeParseException("範囲の開始に対し、終了が記述されていません。key=${key} 行番号=${lines[bgnIdx].lineNo} 開始行=${lines[bgnIdx].line}");
				idx ++;
			}
			dividedNode.end = lines[idx].line;
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
		 * 指定された行の種類を判定します。
		 * @param map 行マップ
		 * @param newLines 種類付き行リスト
		 * @return 種類の判定をしたか否か
		 */
		boolean kindof(Map map, List newLines){
			switch (map.line){
				case {isRangeBgn.call(it)}: map.kind = "${name}#bgn"; break;
				case {isRangeEnd.call(it)}: map.kind = "${name}#end"; break;
				default: return false;
			}
			newLines << map;
			return true;
		}
		
		/**
		 * 行毎にタグ付けをします。
		 * @param lines 種類付き行リスト
		 * @param node タグ付きテキストのノード
		 * @param idx 走査位置
		 * @param endIdx 走査終了位置
		 * @return 新しい走査位置
		 */
		int tagging(List lines, TagText.Node node, int idx, int endIdx){
			if (lines[idx].kind != "${name}#bgn") return idx;
			int bgnIdx = idx;
			TagText.Node enclosedNode = node.newInstanceNode(name);
			node << enclosedNode;
			enclosedNode.bgn = lines[idx].line;
			List stacks = [];
			boolean loop = true;
			while (loop){
				idx ++;
				if (idx > endIdx) throw new WashRangeParseException("範囲の開始に対し、終了が記述されていません。key=${key} 行番号=${lines[bgnIdx].lineNo} 開始行=${lines[bgnIdx].line}");
				switch (lines[idx].kind){
					case "${name}#bgn":
						stacks << idx;
						break;
					case "${name}#end":
						if (stacks.size() == 0){
							enclosedNode.end = lines[idx].line;
							loop = false;
						} else {
							stacks.pop();
						}
						break;
				}
			}
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
			escapeCl = (map.escape instanceof TpacText)? shell.evaluate(map.escape.toString(), "${key}_escape.groovy") : { int idx, int level -> map.escape.refer().call(idx, level) };
		}
		
		/**
		 * 指定された行の種類を判定します。
		 * @param map 行マップ
		 * @param newLines 種類付き行リスト
		 * @return 種類の判定をしたか否か
		 */
		boolean kindof(Map map, List newLines){
			int level = levelCl.call(map.line);
			if (level < 0) return false;
			newLines << [ 'line' : null, 'kind' : "${name}#bgn", 'level' : level ];
			newLines << map;
			return true;
		}
		
		/**
		 * タグ付けの準備をします。
		 * @param lines 種類付き行リスト
		 */
		@Override
		void taggingSetup(List lines){
			if (map.escape instanceof TpacText){
				escapeCl.setProperty('lines', lines);
			} else {
				map.escape.refer().properties['lines'] = lines;
			}
		}
		
		/**
		 * 行毎にタグ付けをします。
		 * @param lines 種類付き行リスト
		 * @param node タグ付きテキストのノード
		 * @param idx 走査位置
		 * @param endIdx 走査終了位置
		 * @return 新しい走査位置
		 */
		int tagging(List lines, TagText.Node node, int idx, int endIdx){
			if (lines[idx].kind != "${name}#bgn") return idx;
			if (node.tags.size() != lines[idx].level) return idx;
			int bgnIdx = idx;
			TagText.Node treeNode = node.newInstanceNode(name);
			node << treeNode;
			while (true){
				idx ++;
				if (idx > endIdx) throw new WashRangeParseException("範囲の開始に対し、終了が記述されていません。key=${key} 行番号=${lines[bgnIdx].lineNo} 開始行=${lines[bgnIdx].line}");
				if (escapeCl.call(idx, lines[bgnIdx].level)) break;
			}
			upper.tagging(lines, treeNode, bgnIdx + 1, idx);
			return idx + 1;
		}
	}
	
	/**
	 * washshスクリプトによる分類の失敗を表す例外です。
	 */
	@InheritConstructors
	class WashRangeParseException extends Exception { }
}
