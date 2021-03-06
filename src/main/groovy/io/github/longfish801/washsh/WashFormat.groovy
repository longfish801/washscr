/*
 * WashFormat.groovy
 *
 * Copyright (C) io.github.longfish801 All Rights Reserved.
 */
package io.github.longfish801.washsh;

import groovy.util.logging.Slf4j;
import io.github.longfish801.shared.ArgmentChecker;
import io.github.longfish801.tpac.element.TeaHandle;
import io.github.longfish801.tpac.element.TpacRefer;
import io.github.longfish801.tpac.element.TpacText;
import io.github.longfish801.tpac.parser.TeaMakerMakeException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * washスクリプトの formatハンドルならびにその下位ハンドルです。
 * @version 1.0.00 2018/09/17
 * @author io.github.longfish801
 */
@Slf4j('LOG')
class WashFormat implements TeaHandle {
	/** GroovyShell */
	static GroovyShell shell = new GroovyShell(WashRange.class.classLoader);
	/** 共通処理対象タグ一覧 */
	List includeCmn;
	/** 共通処理対象外タグ一覧 */
	List excludeCmn;
	
	/**
	 * このハンドラの妥当性を検証します。
	 */
	@Override
	void validate(){
		if (!name.empty) throw new TeaMakerMakeException("formatには名前を指定できません。key=${key}");
		if (map.include != null && !(map.include instanceof List)) throw new TeaMakerMakeException("includeはリストを指定してください。key=${key}");
		if (map.exclude != null && !(map.exclude instanceof List)) throw new TeaMakerMakeException("excludeはリストを指定してください。key=${key}");
		includeCmn = map.include ?: [];
		excludeCmn = map.exclude ?: [ 'masked' ];
	}
	
	/**
	 * テキスト範囲を整形します。
	 * @param textRange テキスト範囲
	 */
	void apply(TextRange textRange){
		Closure scanTextRange;
		scanTextRange = { TextRange curText ->
			if (curText instanceof TextRange.Node){
				curText.lowers.each { scanTextRange.call(it) }
				lowers.values().each { if (it.inRange(curText.name, includeCmn, excludeCmn)) it.editNode(curText) }
			} else {
				lowers.values().each { if (it.inRange(curText.upper?.name, includeCmn, excludeCmn)) curText.lines = it.format(curText) }
			}
		}
		scanTextRange.call(textRange);
	}
	
	/**
	 * replaceハンドルに対応するインスタンスを新規作成します。
	 * @return replaceハンドルに対応するインスタンス
	 */
	WashReplace newInstanceReplace(){
		return new WashReplace();
	}
	
	/**
	 * reprexハンドルに対応するインスタンスを新規作成します。
	 * @return reprexハンドルに対応するインスタンス
	 */
	WashReprex newInstanceReprex(){
		return new WashReprex();
	}
	
	/**
	 * callハンドルに対応するインスタンスを新規作成します。
	 * @return callハンドルに対応するインスタンス
	 */
	WashCall newInstanceCall(){
		return new WashCall();
	}
	
	/**
	 * formatハンドルの下位ハンドルの操作の特性です。
	 */
	trait FormatOperator {
		/** 処理対象タグ一覧 */
		List includeList = [];
		/** 処理対象外タグ一覧 */
		List excludeList = [];
		
		/**
		 * このハンドラの妥当性を検証します。
		 */
		void validate(){
			if (map.include != null){
				if (!(map.include instanceof String) && !(map.include instanceof List)){
					throw new TeaMakerMakeException("includeは文字列あるいはリストで指定してください。key=${key}");
				}
				includeList = (map.include instanceof String)? [ map.include ] : map.include;
			}
			if (map.exclude != null){
				if (!(map.exclude instanceof String) && !(map.exclude instanceof List)){
					throw new TeaMakerMakeException("excludeは文字列あるいはリストで指定してください。key=${key}");
				}
				excludeList = (map.exclude instanceof String)? [ map.exclude ] : map.exclude;
			}
		}
		
		/**
		 * include, exclude指定から指定されたタグが該当するか判定します。
		 * @param tag タグ
		 * @param includeCmn 共通処理対象タグ一覧
		 * @param excludeCmn 共通処理対象外タグ一覧
		 * @return タグが該当するか
		 */
		boolean inRange(String tag, List includeCmn, List excludeCmn){
			List incList = includeList + includeCmn;
			List excList = excludeList + excludeCmn;
			return ((incList.empty || incList.any { it == tag }) && excList.every { it != tag });
		}
		
		/**
		 * ノードを編集します。
		 * @param node テキスト範囲のノード
		 */
		void editNode(TextRange.Node node){
			// なにもしません
		}
		
		/**
		 * テキストを整形します。
		 * @param leaf テキスト範囲のテキスト
		 * @return 整形後の行リスト
		 */
		abstract List format(TextRange.Leaf leaf);
	}
	
	/**
	 * replaceハンドルです。
	 */
	class WashReplace implements TeaHandle, FormatOperator {
		/** 検索語と置換語とのマップ */
		Map repMap;
		
		/**
		 * このハンドラの妥当性を検証します。
		 */
		@Override
		void validate(){
			FormatOperator.super.validate();
			if (text.empty) throw new TeaMakerMakeException("置換文字列が定義されていません。key=${key}");
			repMap = parseTextToMap(text);
		}
		
		/**
		 * テキストを整形します。
		 * @param leaf テキスト範囲のテキスト
		 * @return 整形後の行リスト
		 */
		List format(TextRange.Leaf leaf){
			List newLines = [];
			leaf.lines.each { String line ->
				for (String findWord : repMap.keySet()){
					line = line.replaceAll(Pattern.quote(findWord), Matcher.quoteReplacement(repMap[findWord]));
				}
				newLines << line;
			}
			return newLines;
		}
		
		/**
		 * 指定された文字列を行ごとに分割し、タブを含む行をタブ区切りとみなしてマップを作成します。
		 * @param lines 対象文字列
		 * @return 検索語と置換語とのマップ
		 */
		protected Map parseTextToMap(List lines){
			Map map = [:];
			for (String line in lines){
				int tabIdx = line.indexOf("\t");
				if (tabIdx > 0) map[line.substring(0, tabIdx)] = line.substring(tabIdx + 1);
			}
			return map;
		}
	}
	
	/**
	 * reprexハンドルです。
	 */
	class WashReprex extends WashReplace {
		/**
		 * テキストを整形します。
		 * @param leaf テキスト範囲のテキスト
		 * @return 整形後の行リスト
		 */
		List format(TextRange.Leaf leaf){
			List newLines = [];
			leaf.lines.each { String line ->
				for (String findWord : repMap.keySet()){
					line = line.replaceAll(findWord, repMap[findWord]);
				}
				newLines << line;
			}
			return newLines;
		}
	}
	
	/**
	 * callハンドルです。
	 */
	class WashCall implements TeaHandle, FormatOperator {
		/** bgnクロージャ */
		Closure bgnCl;
		/** endクロージャ */
		Closure endCl;
		/** textクロージャ */
		Closure textCl;
		
		/**
		 * このハンドラの妥当性を検証します。
		 */
		@Override
		void validate(){
			FormatOperator.super.validate();
			if (map.size() == 0) throw new TeaMakerMakeException("整形処理のクロージャを最低ひとつ定義してください。key=${key}");
			if (map.bgn != null){
				if (!(map.bgn instanceof TpacRefer) && !(map.bgn instanceof TpacText)){
					throw new TeaMakerMakeException("bgnは参照あるいはテキストで定義してください。key=${key}");
				}
				bgnCl = (map.bgn instanceof TpacText)? shell.evaluate(map.bgn.toString(), "${key}_bgn.groovy") : { Map labels -> map.bgn.refer().call(labels) };
			}
			if (map.end != null){
				if (!(map.end instanceof TpacRefer) && !(map.end instanceof TpacText)){
					throw new TeaMakerMakeException("endは参照あるいはテキストで定義してください。key=${key}");
				}
				endCl = (map.end instanceof TpacText)? shell.evaluate(map.end.toString(), "${key}_end.groovy") : { Map labels -> map.end.refer().call(labels) };
			}
			if (map.text != null){
				if (!(map.text instanceof TpacRefer) && !(map.text instanceof TpacText)){
					throw new TeaMakerMakeException("textは参照あるいはテキストで定義してください。key=${key}");
				}
				textCl = (map.text instanceof TpacText)? shell.evaluate(map.text.toString(), "${key}_text.groovy") : { List lines, Map labels -> map.text.refer().call(lines, labels) };
			}
		}
		
		/**
		 * ノードを編集します。
		 * @param node テキスト範囲のノード
		 */
		@Override
		void editNode(TextRange.Node node){
			if (bgnCl != null) node.labels.bgn = bgnCl.call(node.labels);
			if (endCl != null) node.labels.end = endCl.call(node.labels);
		}
		
		/**
		 * テキストを整形します。
		 * @param leaf テキスト範囲のテキスト
		 * @return 整形後の行リスト
		 */
		List format(TextRange.Leaf leaf){
			return textCl?.call(leaf.lines, leaf.upper.labels) ?: leaf.lines;
		}
	}
}
