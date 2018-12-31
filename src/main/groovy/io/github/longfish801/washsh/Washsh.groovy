/*
 * Washsh.groovy
 *
 * Copyright (C) io.github.longfish801 All Rights Reserved.
 */
package io.github.longfish801.washsh;

import groovy.transform.InheritConstructors;
import groovy.util.logging.Slf4j;
import io.github.longfish801.clmap.Clinfo;
import io.github.longfish801.clmap.Clmap;
import io.github.longfish801.tpac.element.TeaDec;
import io.github.longfish801.tpac.parser.TeaMakerMakeException;
import java.util.regex.Pattern;

/**
 * washsh記法に沿って文字列を変換します。
 * @version 1.0.00 2018/09/17
 * @author io.github.longfish801
 */
@Slf4j('LOG')
class Washsh implements TeaDec {
	/**
	 * washsh記法に沿ってファイル内の文字列を変換します。<br>
	 * 文字コードは環境変数file.encodingで指定された値を使用します。
	 * @param file 処理対象ファイル
	 * @return 変換結果
	 */
	String wash(File file){
		return wash(file, System.getProperty('file.encoding'));
	}
	
	/**
	 * washsh記法に沿ってファイル内の文字列を変換します。
	 * @param file 処理対象ファイル
	 * @param enc Washshを記述したファイルの文字コード
	 * @return 変換結果
	 */
	String wash(File file, String enc){
		return wash(new BufferedReader(new InputStreamReader(new FileInputStream(file), enc)));
	}
	
	/**
	 * washsh記法に沿って文字列を変換します。
	 * @param text 処理対象文字列
	 * @return 変換結果
	 */
	String wash(String text){
		return wash(new BufferedReader(new StringReader(text)));
	}
	
	/**
	 * washsh記法に沿って BufferedReaderから読みこんだ文字列を変換します。
	 * @param reader 処理対象のBufferedReader
	 * @return 変換結果
	 */
	String wash(BufferedReader reader){
		StringWriter writer = new StringWriter();
		wash(reader, new BufferedWriter(writer));
		return writer.toString();
	}
	
	/**
	 * washsh記法に沿って BufferedReaderから読みこんだ文字列を変換し、BufferdWriterに出力します。
	 * @param reader 処理対象のBufferedReader
	 * @param writer 処理結果のBufferdWriter
	 * @throws WashshRuntimeException washshスクリプト実行中に問題が起きました。
	 */
	void wash(BufferedReader reader, BufferedWriter writer){
		LOG.debug('washsh実行開始 key={}', key);
		try {
			// 処理対象をテキスト範囲に変換します
			TextRange.Node node = new TextRange().newInstanceNode('');
			if (lowers['range:'] == null){
				node << node.newInstanceLeaf();
				reader.eachLine { node.lowers.last() << it }
			} else {
				List lines = [];
				reader.eachLine { lines << it }
				lowers['range:'].tagging(lines, node, 0, lines.size() - 1);
			}
			// 整形します
			lowers['format:']?.apply(node);
			// 出力します
			writer.withWriter { Writer wrtr -> node.write(wrtr) }
		} catch (exc){
			throw new WashshRuntimeException("washshスクリプト実行中に問題が起きました。key=${key}", exc);
		}
		LOG.debug("washsh実行終了 key={}", key);
	}
	
	/**
	 * washshスクリプトの実行失敗を表す例外です。
	 */
	@InheritConstructors
	class WashshRuntimeException extends Exception {}
}
