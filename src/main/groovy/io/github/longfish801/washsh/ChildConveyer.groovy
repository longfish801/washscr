/*
 * ChildConveyer.groovy
 *
 * Copyright (C) io.github.longfish801 All Rights Reserved.
 */
package io.github.longfish801.washsh;

import groovy.util.logging.Slf4j;
import io.github.longfish801.clmap.Clmap;
import io.github.longfish801.shared.lang.ArgmentChecker;
import io.github.longfish801.washsh.conveyer.Conveyer;
import io.github.longfish801.washsh.conveyer.ConveyerSystem;
import io.github.longfish801.washsh.conveyer.Product;
import io.github.longfish801.tpac.element.TpacChild;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Washshにおける子要素に対応する工程です。
 * @version 1.0.00 2017/07/28
 * @author io.github.longfish801
 */
@Slf4j('LOG')
class ChildConveyer {
	/** Clmap */
	Clmap clmap = null;
	
	/**
	 * コンストラクタ。
	 * @param clmap Clmap
	 */
	ChildConveyer(Clmap clmap){
		this.clmap = clmap;
	}
	
	/**
	 * 子要素のリストから子工程のリストを生成して返します。
	 * @param childs 子要素のリスト
	 * @return 子工程のリスト
	 */
	List<Conveyer<List<String>>> getConveyers(List<TpacChild> childs){
		List<Conveyer<List<String>>> conveyers = [];
		childs.each {
			Conveyer<List<String>> conveyer = getConveyer(it);
			if (conveyer != null) conveyers << conveyer;
		}
		return conveyers;
	}
	
	/**
	 * 子要素のConveyerインスタンスを返します。
	 *
	 * @param childType 子要素
	 * @param config ConfigObject
	 * @return Conveyerインスタンス
	 */
	Conveyer<List<String>> getConveyer(TpacChild child){
		Conveyer conveyer = null;
		switch (child.tag){
			case 'replace':
				conveyer = new ReplaceConveyer<List<String>>(child);
				break;
			case 'reprex':
				conveyer = new ReprexConveyer<List<String>>(child);
				break;
			case 'call':
				conveyer = new CallConveyer<List<String>>(child);
				break;
			default:
				// 上記以外のタグは無視します
				break;
		}
		return conveyer;
	}
	
	/**
	 * 位置情報を保持する工程の抽象クラスです。
	 */
	abstract class IndexedConveyer<List> extends Conveyer {
		/** judge属性 */
		String judgeCombi = null;
		/** first属性 */
		String firstCombi = null;
		/** last属性 */
		String lastCombi = null;
		
		/** {@inheritDoc} */
		@Override
		void convey(Product<List<String>> product){
			IndexedProduct indexedProduct = product as IndexedProduct;
			clmap.config."${name}".index = indexedProduct.index;
			clmap.config."${name}".isLast = indexedProduct.isLast;
			super.convey(product);
		}
	}
	
	/**
	 * replaceタグの工程です。
	 */
	class ReplaceConveyer<List> extends IndexedConveyer {
		/** 検索語と置換語とのマップ */
		Map repMap = null;
		
		/**
		 * コンストラクタ。
		 * @param child 子要素
		 */
		ReplaceConveyer(TpacChild child){
			this.name = child.key;
			this.judgeCombi = child.attrs['judge'];
			if (this.judgeCombi != null) ArgmentChecker.checkNotNull('judge属性に該当するクロージャ定義', clmap.cl(this.judgeCombi));
			this.firstCombi = child.attrs['first'];
			if (this.firstCombi != null) ArgmentChecker.checkNotNull('first属性に該当するクロージャ定義', clmap.cl(this.firstCombi));
			this.lastCombi = child.attrs['last'];
			if (this.lastCombi != null) ArgmentChecker.checkNotNull('last属性に該当するクロージャ定義', clmap.cl(this.lastCombi));
			this.repMap = parseTextToMap(child.text);
			ArgmentChecker.checkNotEmptyMap('検索文字列と置換後文字列の定義', this.repMap);
		}
		
		/** {@inheritDoc} */
		@Override
		List<String> process(List<String> elem){
			List<String> newLines = [];
			try {
				// 処理対象とするか判定し、処理対象としない場合はそのまま返します
				if (judgeCombi != null && !clmap.cl(judgeCombi).call(elem)) return elem;
				// 前処理をします
				if (firstCombi != null) elem = clmap.cl(firstCombi).call(elem);
				
				// 固定文字列で置換します
				elem.each { String line ->
					for (String findWord : repMap.keySet()){
						line = line.replaceAll(Pattern.quote(findWord), Matcher.quoteReplacement(repMap[findWord]));
					}
					newLines << line;
				}
				
				// 後処理をします
				if (lastCombi != null) newLines = clmap.cl(lastCombi).call(newLines);
				
				LOG.debug('子工程 {} :{}', name, newLines);
			} catch (exc){
				LOG.warn('子工程での加工時に問題が発生しました。name={}', name);
				throw exc;
			}
			return newLines;
		}
	}
	
	/**
	 * reprexタグの工程です。
	 */
	class ReprexConveyer<List> extends IndexedConveyer {
		/** 検索語と置換語とのマップ */
		Map repMap = null;
		
		/**
		 * コンストラクタ。
		 * @param child 子要素
		 */
		ReprexConveyer(TpacChild child){
			this.name = child.key;
			this.judgeCombi = child.attrs['judge'];
			if (this.judgeCombi != null) ArgmentChecker.checkNotNull('judge属性に該当するクロージャ定義', clmap.cl(this.judgeCombi));
			this.firstCombi = child.attrs['first'];
			if (this.firstCombi != null) ArgmentChecker.checkNotNull('first属性に該当するクロージャ定義', clmap.cl(this.firstCombi));
			this.lastCombi = child.attrs['last'];
			if (this.lastCombi != null) ArgmentChecker.checkNotNull('last属性に該当するクロージャ定義', clmap.cl(this.lastCombi));
			this.repMap = parseTextToMap(child.text);
			ArgmentChecker.checkNotEmptyMap('検索文字列と置換後文字列の定義', this.repMap);
		}
		
		/** {@inheritDoc} */
		@Override
		List<String> process(List<String> elem){
			List<String> newLines = [];
			try {
				// 処理対象とするか判定し、処理対象としない場合はそのまま返します
				if (judgeCombi != null && !clmap.cl(judgeCombi).call(elem)) return elem;
				// 前処理をします
				if (firstCombi != null) elem = clmap.cl(firstCombi).call(elem);
				
				// 正規表現で置換します
				elem.each { String line ->
					for (String findWord : repMap.keySet()){
						line = line.replaceAll(findWord, repMap[findWord]);
					}
					newLines << line;
				}
				
				// 後処理をします
				if (lastCombi != null) newLines = clmap.cl(lastCombi).call(newLines);
				
				LOG.debug('子工程 {} :{}', name, newLines);
			} catch (exc){
				LOG.warn('子工程での加工時に問題が発生しました。name={}', name);
				throw exc;
			}
			return newLines;
		}
	}
	
	/**
	 * callタグの工程です。
	 */
	class CallConveyer<List> extends IndexedConveyer {
		/** combi属性 */
		String combi = null;
		
		/**
		 * コンストラクタ。
		 * @param child 子要素
		 */
		CallConveyer(TpacChild child){
			this.name = child.key;
			this.combi = child.attrs['combi'];
			ArgmentChecker.checkNotNull('combi属性', this.combi);
			ArgmentChecker.checkNotNull('combi属性に該当するクロージャ定義', clmap.cl(this.combi));
			this.judgeCombi = child.attrs['judge'];
			if (this.judgeCombi != null) ArgmentChecker.checkNotNull('judge属性に該当するクロージャ定義', clmap.cl(this.judgeCombi));
			this.firstCombi = child.attrs['first'];
			if (this.firstCombi != null) ArgmentChecker.checkNotNull('first属性に該当するクロージャ定義', clmap.cl(this.firstCombi));
			this.lastCombi = child.attrs['last'];
			if (this.lastCombi != null) ArgmentChecker.checkNotNull('last属性に該当するクロージャ定義', clmap.cl(this.lastCombi));
		}
		
		/** {@inheritDoc} */
		@Override
		List<String> process(List<String> elem){
			List<String> newLines;
			try {
				// 処理対象とするか判定し、処理対象としない場合はそのまま返します
				if (judgeCombi != null && !clmap.cl(judgeCombi).call(elem)) return elem;
				// 前処理をします
				if (firstCombi != null) elem = clmap.cl(firstCombi).call(elem);
				
				// クロージャを呼びます
				newLines = clmap.cl(combi).call(elem);
				
				// 後処理をします
				if (lastCombi != null) newLines = clmap.cl(lastCombi).call(newLines);
				
				LOG.debug('子工程 {} :{}', name, newLines);
			} catch (exc){
				LOG.error('子工程での加工時に問題が発生しました。name={}', name);
				throw exc;
			}
			return newLines;
		}
	}
	
	/**
	 * 指定された文字列を行ごとに分割し、タブを含む行をタブ区切りとみなしてマップを作成します。
	 * @param text 対象文字列
	 * @return マップ
	 */
	protected Map parseTextToMap(String text){
		Map map = new LinkedHashMap();
		List lines = text.split(/[\r\n]+/);
		for (String line in lines){
			int tabIdx = line.indexOf("\t");
			if (tabIdx > 0) map[line.substring(0, tabIdx)] = line.substring(tabIdx + 1);
		}
		return map;
	}
}
