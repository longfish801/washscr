/*
 * WashMaker.groovy
 *
 * Copyright (C) io.github.longfish801 All Rights Reserved.
 */
package io.github.longfish801.washsh;

import groovy.util.logging.Slf4j;
import io.github.longfish801.shared.ExchangeResource;
import io.github.longfish801.tpac.element.TeaDec;
import io.github.longfish801.tpac.element.TeaHandle;
import io.github.longfish801.tpac.parser.TeaMaker;
import io.github.longfish801.tpac.parser.TeaMakerMakeException;

/**
 * washsh記法の文字列の解析にともない、各要素を生成します。
 * @version 1.0.00 2018/09/13
 * @author io.github.longfish801
 */
@Slf4j('LOG')
class WashMaker implements TeaMaker {
	/** ConfigObject */
	static final ConfigObject cnstWashMaker = ExchangeResource.config(WashMaker.class);
	/** rangeハンドル */
	WashRange range = null;
	/** formatハンドル */
	WashFormat format = null;
	
	/**
	 * TeaDecインスタンスを生成します。
	 * @param tag タグ
	 * @param name 名前
	 * @return TeaDec
	 */
	TeaDec newTeaDec(String tag, String name){
		return new Washsh();
	}
	
	/**
	 * TeaHandleインスタンスを生成します。
	 * @param tag タグ
	 * @param name 名前
	 * @param upper 上位ハンドル
	 * @return TeaHandle
	 */
	TeaHandle newTeaHandle(String tag, String name, TeaHandle upper){
		if (!cnstWashMaker.check.valids.contains(tag)) throw new TeaMakerMakeException("不正なタグ名です。tag=${tag}, name=${name}");
		if (!cnstWashMaker.check.hierarchy[upper.tag].contains(tag)) throw new TeaMakerMakeException("タグの親子関係が不正です。tag=${tag}, name=${name}, upper=${upper.tag}");
		TeaHandle handle;
		switch (tag){
			case 'range': range = new WashRange(); handle = range; break;
			case 'format': format = new WashFormat(); handle = format; break;
			case 'mask': handle = range.newInstanceMask(); break;
			case 'divided': handle = range.newInstanceDivided(); break;
			case 'enclosed': handle = range.newInstanceEnclosed(); break;
			case 'tree': handle = range.newInstanceTree(); break;
			case 'replace': handle = format.newInstanceReplace(); break;
			case 'reprex': handle = format.newInstanceReprex(); break;
			case 'call': handle = format.newInstanceCall(); break;
		}
		return handle;
	}
}
