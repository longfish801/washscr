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
		excludeCmn = map.exclude ?: [];
	}
	
	/**
	 * タグ付きテキストを整形します。
	 * @param list タグ付きテキストのリスト
	 */
	void apply(List<TagText> list){
		list.each { TagText tagText ->
			lowers.values().each {
				if (it.inRange(tagText.tags, includeCmn, excludeCmn)) tagText.text = it.format(tagText.text)
			}
		}
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
	 * deleteハンドルに対応するインスタンスを新規作成します。
	 * @return deleteハンドルに対応するインスタンス
	 */
	WashDelete newInstanceDelete(){
		return new WashDelete();
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
		 * テキストを整形します。
		 * @param text テキスト
		 * @return 整形結果
		 */
		abstract String format(String text);
		
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
		 * @param tags タグ一覧
		 * @param includeCmn 共通処理対象タグ一覧
		 * @param excludeCmn 共通処理対象外タグ一覧
		 * @return タグが該当するか
		 */
		boolean inRange(List<String> tags, List includeCmn, List excludeCmn){
			List incList = includeList + includeCmn;
			List excList = excludeList + excludeCmn;
			return ((incList.empty || incList.any { tags.contains(it) }) && excList.every { !tags.contains(it) });
		}
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
		 * @param text テキスト
		 * @return 整形結果
		 */
		String format(String text){
			for (String findWord : repMap.keySet()){
				text = text.replaceAll(Pattern.quote(findWord), Matcher.quoteReplacement(repMap[findWord]));
			}
			return text;
		}
		
		/**
		 * 指定された文字列を行ごとに分割し、タブを含む行をタブ区切りとみなしてマップを作成します。
		 * @param text 対象文字列
		 * @return マップ
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
		 * @param text テキスト
		 * @return 整形結果
		 */
		@Override
		String format(String text){
			for (String findWord : repMap.keySet()){
				text = text.replaceAll(findWord, repMap[findWord]);
			}
			return text;
		}
	}
	
	/**
	 * callハンドルです。
	 */
	class WashCall implements TeaHandle, FormatOperator {
		/** クロージャ */
		Closure formatCl;
		
		/**
		 * このハンドラの妥当性を検証します。
		 */
		@Override
		void validate(){
			FormatOperator.super.validate();
			if (text.empty) throw new TeaMakerMakeException("置換文字列が定義されていません。key=${key}");
			if ((scalar == null || !(scalar instanceof TpacRefer)) && text.empty){
				throw new TeaMakerMakeException("整形処理のクロージャを参照あるいはテキストで定義してください。key=${key}");
			}
			formatCl = (scalar instanceof TpacRefer)? scalar.refer() : shell.evaluate(text.toString(), "${key}_call.groovy");
		}
		
		/**
		 * テキストを整形します。
		 * @param text テキスト
		 * @return 整形結果
		 */
		String format(String text){
			return formatCl.call(text);
		}
	}
	
	/**
	 * deleteハンドルです。
	 */
	class WashDelete implements TeaHandle, FormatOperator {
		/**
		 * このハンドラの妥当性を検証します。
		 */
		@Override
		void validate(){
			FormatOperator.super.validate();
		}
		
		/**
		 * テキストを整形します。
		 * @param text テキスト
		 * @return 整形結果
		 */
		String format(String text){
			return null;
		}
	}
}
