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
	/** マスキング箇所の開始か否か判定するクロージャ */
	Closure isMaskBgn;
	/** マスキング箇所の終了か否か判定するクロージャ */
	Closure isMaskEnd;
	
	/**
	 * このハンドラの妥当性を検証します。
	 */
	@Override
	void validate(){
		if (map.masking != null && map.masking.bgn == null) throw new TeaMakerMakeException("masking指定時は bgn指定が必須です。key=${key}");
		if (map.masking != null){
			if (map.masking.bgn != null && (!(map.masking.bgn instanceof String) && !(map.masking.bgn instanceof Pattern))) {
				throw new TeaMakerMakeException("bgnは文字列あるいは正規表現を指定してください。key=${key}");
			}
			if (map.masking.end != null && (!(map.masking.end instanceof String) && !(map.masking.end instanceof Pattern))) {
				throw new TeaMakerMakeException("endは文字列あるいは正規表現を指定してください。key=${key}");
			}
			isMaskBgn = { String line ->
				return ((map.masking.bgn instanceof String && line == map.masking.bgn)
				     || (map.masking.bgn instanceof Pattern && line ==~ map.masking.bgn));
			}
			isMaskEnd = { String line, String bgnLine ->
				return ((map.masking.end == null && line == bgnLine)
				     || (map.masking.end instanceof String && line == map.masking.end)
				     || (map.masking.end instanceof Pattern && line ==~ map.masking.end));
			}
		}
	}
	
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
			// 行毎に分類します
			TagLines tagLines = kindof(reader);
			// タグ付けをします
			for (int idx in 0..<tagLines.lines.size()){
				tagLines.appendTag(idx, (tagLines.kinds[idx] ==~ /masked.*/)? tagLines.kinds[idx] : null);
			}
			lowers['range:']?.tagging(tagLines);
			// 範囲毎にテキストを分割します
			List<TagText> list = [];
			List preTags;
			for (int idx in 0..<tagLines.lines.size()){
				if (tagLines.tags[idx] != preTags){
					list << new TagText(tagLines.tags[idx]);
					preTags = tagLines.tags[idx];
				}
				list.last().text += "${tagLines.lines[idx]}\n";
			}
			// 整形します
			if (lowers['format:'] != null){
				if (map.masking != null) lowers['format:'].excludeCmn << 'masked';
				lowers['format:'].apply(list);
			}
			// 出力します
			writer.withWriter { Writer wrtr ->
				list.each { TagText tagText ->
					if (tagText.text != null) wrtr << tagText.text.replaceAll("\n", System.lineSeparator());
				}
			}
		} catch (exc){
			throw new WashshRuntimeException("washshスクリプト実行中に問題が起きました。key=${key}", exc);
		}
		LOG.debug("washsh実行終了 key={}", key);
	}
	
	/**
	 * BufferedReaderから読みこんだ文字列を行毎に分類します。
	 * @param reader 処理対象のBufferedReader
	 * @return 分類結果
	 */
	protected TagLines kindof(BufferedReader reader){
		TagLines tagLines = new TagLines();
		Closure kindof = { String line ->
			if (line.empty) return 'empty';
			return lowers['range:']?.kindof(line);
		}
		if (map.masking == null){
			reader.eachLine { String line ->
				tagLines.append(line, kindof.call(line));
			}
		} else {
			boolean inMask = false;
			String bgnLine;
			reader.eachLine { String line ->
				String kind;
				if (inMask){
					if (isMaskEnd.call(line, bgnLine)){
						kind = 'masked#end';
						bgnLine = null;
						inMask = false;
					} else {
						kind = 'masked';
					}
				} else {
					if (isMaskBgn.call(line)){
						kind = 'masked#bgn';
						bgnLine = line;
						inMask = true;
					} else {
						kind = kindof.call(line);
					}
				}
				tagLines.append(line, kind);
			}
			if (inMask) throw new WashshParseException("マスキング箇所の開始に対し、終了が記述されていません。key=${key}");
		}
		return tagLines;
	}
	
	/**
	 * washshスクリプトの実行失敗を表す例外です。
	 */
	@InheritConstructors
	class WashshRuntimeException extends Exception {}
	
	/**
	 * washshスクリプトの解析失敗を表す例外です。
	 */
	@InheritConstructors
	class WashshParseException extends Exception {}
}
