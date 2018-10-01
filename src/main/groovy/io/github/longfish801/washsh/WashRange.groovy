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
	 * @param line 行
	 * @return 種類
	 */
	String kindof(String line){
		String kind;
		for (def hndl in lowers.values()){
			if ((kind = hndl.kindof(line)) != null) return kind;
		}
		return null;
	}
	
	/**
	 * 行毎にタグ付けをします。
	 * @param tagLines タグ付き行リスト
	 */
	void tagging(TagLines tagLines){
		lowers.values().each { it.taggingSetup(tagLines) };
		tagging(tagLines, 0, tagLines.lines.size() - 1);
	}
	
	/**
	 * 行毎にタグ付けをします。
	 * @param tagLines タグ付き行リスト
	 * @param bgnIdx 走査開始位置
	 * @param endIdx 走査終了位置
	 */
	void tagging(TagLines tagLines, int bgnIdx, int endIdx){
		int idx = bgnIdx;
		int preIdx;
		while (idx <= endIdx){
			preIdx = idx;
			// マスキング箇所を飛ばします
			if (tagLines.kinds[idx] == 'masked#bgn'){
				while (tagLines.kinds[idx] != 'masked#end') idx ++;
			}
			// 下位ハンドルによるタグ付けをします
			lowers.values().each { idx = it.tagging(tagLines, idx, endIdx) };
			if (idx == preIdx) idx ++;
		}
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
	 * complexハンドルに対応するインスタンスを新規作成します。
	 * @return complexハンドルに対応するインスタンス
	 */
	WashComplex newInstanceComplex(){
		return new WashComplex();
	}
	
	/**
	 * rangeハンドルの下位ハンドルの操作の特性です。
	 */
	trait RangeOperator {
		/**
		 * 指定された行の種類を判定します。
		 * @param line 行
		 * @return 種類
		 */
		abstract String kindof(String line);
		
		/**
		 * タグ付けの準備をします。
		 * @param tagLines タグ付き行リスト
		 */
		void taggingSetup(TagLines tagLines){
			// なにもしません
		}
		
		/**
		 * 行毎にタグ付けをします。
		 * @param tagLines タグ付き行リスト
		 * @param idx 走査位置
		 * @param endIdx 走査終了位置
		 * @return 走査位置
		 */
		abstract int tagging(TagLines tagLines, int idx, int endIdx);
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
		 * @param line 行
		 * @return 種類
		 */
		String kindof(String line){
			return (isRangeDiv.call(line))? "${name}#div" : null;
		}
		
		/**
		 * 行毎にタグ付けをします。
		 * @param tagLines タグ付き行リスト
		 * @param idx 走査位置
		 * @param endIdx 走査終了位置
		 * @return 走査位置
		 */
		int tagging(TagLines tagLines, int idx, int endIdx){
			if (tagLines.kinds[idx] != "${name}#div") return idx;
			int bgnIdx = idx + 1;
			tagLines.appendTag(idx, "${name}#bgn");
			while (true){
				idx ++;
				if (idx > endIdx) throw new WashRangeParseException("範囲の開始(行番号:${bgnIdx})に対し、終了が記述されていません。key=${key}");
				if (tagLines.kinds[idx] == "${name}#div"){
					tagLines.appendTag(idx, "${name}#end");
					break;
				} else {
					tagLines.appendTag(idx, "${name}");
				}
			}
			upper.tagging(tagLines, bgnIdx, idx - 1);
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
		 * @param line 行
		 * @return 種類
		 */
		String kindof(String line){
			switch (line){
				case {isRangeBgn.call(it)}: return "${name}#bgn";
				case {isRangeEnd.call(it)}: return "${name}#end";
			}
			return null;
		}
		
		/**
		 * 行毎にタグ付けをします。
		 * @param tagLines タグ付き行リスト
		 * @param idx 走査位置
		 * @param endIdx 走査終了位置
		 * @return 走査位置
		 */
		int tagging(TagLines tagLines, int idx, int endIdx){
			if (tagLines.kinds[idx] != "${name}#bgn") return idx;
			int bgnIdx = idx + 1;
			tagLines.appendTag(idx, "${name}#bgn");
			List stacks = [];
			boolean loop = true;
			while (loop){
				idx ++;
				if (idx > endIdx) throw new WashRangeParseException("範囲の開始(行番号:${bgnIdx})に対し、終了が記述されていません。key=${key}");
				switch (tagLines.kinds[idx]){
					case "${name}#bgn":
						stacks << idx;
						break;
					case "${name}#end":
						if (stacks.size() == 0){
							tagLines.appendTag(idx, "${name}#end");
							loop = false;
						} else {
							stacks.pop();
						}
						break;
					default:
						tagLines.appendTag(idx, "${name}");
				}
			}
			upper.tagging(tagLines, bgnIdx, idx - 1);
			return idx + 1;
		}
	}
	
	/**
	 * complexハンドルです。
	 */
	class WashComplex implements TeaHandle, RangeOperator {
		/** enterクロージャ */
		Closure enter;
		/** escapeクロージャ */
		Closure escape;
		
		/**
		 * このハンドラの妥当性を検証します。
		 */
		@Override
		void validate(){
			if (name.empty) throw new TeaMakerMakeException("ハンドルに名前が定義されていません。key=${key}");
			if (map.kindof == null) throw new TeaMakerMakeException("kindofが定義されていません。key=${key}");
			if (!(map.kindof instanceof Map)) throw new TeaMakerMakeException("kindofにはマップを指定してください。key=${key}");
			if (map.kindof.values().any { !(it instanceof String) && !(it instanceof Pattern) }){
				throw new TeaMakerMakeException("kindofの値は文字列あるいは正規表現で指定してください。key=${key}");
			}
			if (map.enter == null || (!(map.enter instanceof TpacRefer) && !(map.enter instanceof TpacText))){
				throw new TeaMakerMakeException("enterを参照あるいはテキストで定義してください。key=${key}");
			}
			if (map.escape == null || (!(map.escape instanceof TpacRefer) && !(map.escape instanceof TpacText))){
				throw new TeaMakerMakeException("escapeを参照あるいはテキストで定義してください。key=${key}");
			}
			enter = (map.enter instanceof TpacRefer)? map.enter.refer() : shell.evaluate(map.enter.toString(), "${key}_enter.groovy");
			escape = (map.escape instanceof TpacRefer)? map.escape.refer() : shell.evaluate(map.escape.toString(), "${key}_escape.groovy");
		}
		
		/**
		 * 指定された行の種類を判定します。
		 * @param line 行
		 * @return 種類
		 */
		String kindof(String line){
			return map.kindof.find { (it.value instanceof String && line == it.value) || (it.value instanceof Pattern && line ==~ it.value) }?.key;
		}
		
		/**
		 * タグ付けの準備をします。
		 * @param tagLines タグ付き行リスト
		 */
		@Override
		void taggingSetup(TagLines tagLines){
			enter.setProperty('kinds', tagLines.kinds);
			escape.setProperty('kinds', tagLines.kinds);
		}
		
		/**
		 * 行毎にタグ付けをします。
		 * @param tagLines タグ付き行リスト
		 * @param idx 走査位置
		 * @param endIdx 走査終了位置
		 * @return 走査位置
		 */
		int tagging(TagLines tagLines, int idx, int endIdx){
			if (!enter.call(idx)) return idx;
			int bgnIdx = idx + 1;
			tagLines.appendTag(idx, "${name}");
			while (true){
				idx ++;
				if (idx > endIdx) throw new WashRangeParseException("範囲の開始(行番号:${bgnIdx})に対し、終了が記述されていません。key=${key}");
				if (escape.call(idx)){
					break;
				} else {
					tagLines.appendTag(idx, "${name}");
				}
			}
			upper.tagging(tagLines, bgnIdx, idx);
			return idx + 1;
		}
	}
	
	/**
	 * washshスクリプトによる分類の失敗を表す例外です。
	 */
	@InheritConstructors
	class WashRangeParseException extends Exception { }
}
