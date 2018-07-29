/*
 * WashshMaker.groovy
 *
 * Copyright (C) io.github.longfish801 All Rights Reserved.
 */
package io.github.longfish801.washsh;

import groovy.util.logging.Slf4j;
import io.github.longfish801.clmap.ClmapMaker;
import io.github.longfish801.shared.util.ClassSlurper;
import io.github.longfish801.shared.util.TextUtil;
import io.github.longfish801.tpac.TpacMaker;
import io.github.longfish801.tpac.parser.ParseException;
import org.apache.commons.lang3.StringUtils;

/**
 * Washsh記法の文字列の解析にともない、各要素を生成します。
 * @version 1.0.00 2017/07/27
 * @author io.github.longfish801
 */
@Slf4j('LOG')
class WashshMaker extends TpacMaker {
	/** ConfigObject */
	protected static final ConfigObject constants = ClassSlurper.getConfig(WashshMaker.class);
	/** ClmapMakerのConfigObject */
	protected static final ConfigObject constantsCl = ClassSlurper.getConfig(ClmapMaker.class);
	
	/** {@inheritDoc} */
	void createRoot(String tag, String name, int lineNo){
		if (tag != constants.validTags.dec) throw new ParseException("宣言のタグ名が不正です。tag=${tag}, lineNo=${lineNo}");
		super.createRoot(tag, name, lineNo);
	}
	
	/** {@inheritDoc} */
	void createParent(String tag, String name, int lineNo){
		if (constants.validTags.parent.every { it != tag } && constantsCl.validTags.parent.every { it != tag }) throw new ParseException("親要素のタグ名が不正です。tag=${tag}, lineNo=${lineNo}");
		if (!name.empty && constantsCl.nonameTags.parent.any { it == tag }) throw new ParseException("この親要素には名前を指定できません。tag=${tag}, name=${name}, lineNo=${lineNo}");
		super.createParent(tag, name, lineNo);
	}
	
	/** {@inheritDoc} */
	void createChild(String tag, String name, int lineNo){
		if (constants.validTags.child.every { it != tag } && constantsCl.validTags.child.every { it != tag }) throw new ParseException("子要素のタグ名が不正です。tag=${tag}, lineNo=${lineNo}");
		if (!name.empty && constantsCl.nonameTags.child.any { it == tag }) throw new ParseException("この親要素には名前を指定できません。tag=${tag}, name=${name}, lineNo=${lineNo}");
		super.createChild(tag, name, lineNo);
	}
	
	/** {@inheritDoc} */
	void createAttr(String key, String value, int lineNo){
		List validAttrKeys = constants.validAttrKeys[currentElem().tag];
		if (validAttrKeys != null && validAttrKeys.every { it != key }) throw new ParseException("この要素には指定できない属性です。tag=${currentElem().tag}, key=${key}, lineNo=${lineNo}");
		List validAttrVals = constants.validAttrVals[key];
		if (validAttrVals != null && validAttrVals.every { it != value }) throw new ParseException("この要素には指定できない属性値です。tag=${currentElem().tag}, key=${key}, value=${value}, lineNo=${lineNo}");
		super.createAttr(key, value, lineNo);
	}
	
	/** {@inheritDoc} */
	void createText(String text, int lineNo){
		List lines = TextUtil.parseTextLines(text);
		if (lines.any { StringUtils.trimToNull(it) != null }) super.createText(lines.join("\n"), lineNo);
	}
}
