/*
 * WashServer.groovy
 *
 * Copyright (C) io.github.longfish801 All Rights Reserved.
 */
package io.github.longfish801.washsh;

import groovy.util.logging.Slf4j;
import io.github.longfish801.clmap.ClmapMaker;
import io.github.longfish801.tpac.TeaServer;
import io.github.longfish801.tpac.parser.TeaMaker;

/**
 * washshのサーバーです。
 * @version 1.0.00 2018/09/12
 * @author io.github.longfish801
 */
@Slf4j('LOG')
class WashServer implements TeaServer {
	/**
	 * 宣言のタグに対応する TeaMakerを返します。
	 * @param tag 宣言のタグ
	 * @return TeaMaker
	 */
	TeaMaker maker(String tag){
		TeaMaker maker;
		switch (tag){
			case 'washsh': maker = new WashMaker(); break;
			case 'clmap': maker = new ClmapMaker(); break;
			default: maker = TeaServer.super.maker(tag);
		}
		return maker;
	}
}
