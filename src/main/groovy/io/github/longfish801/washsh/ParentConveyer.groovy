/*
 * ParentConveyer.groovy
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
import io.github.longfish801.tpac.element.TpacParent;
import java.lang.Thread as JavaThread;

/**
 * Washshにおける親要素に対応する工程です。
 * @version 1.0.00 2017/07/28
 * @author io.github.longfish801
 */
@Slf4j('LOG')
class ParentConveyer {
	/** Clmap */
	Clmap clmap = null;
	
	/**
	 * コンストラクタ。
	 * @param clmap Clmap
	 */
	ParentConveyer(Clmap clmap){
		this.clmap = clmap;
	}
	
	/**
	 * 親要素のリストから親工程のリストを生成して返します。
	 * @param parents 親要素のリスト
	 * @return 親工程のリスト
	 */
	List<Conveyer<String>> getConveyers(List<TpacParent> parents){
		List<Conveyer<String>> conveyers = [];
		parents.each {
			Conveyer<String> conveyer = getConveyer(it);
			if (conveyer != null) conveyers << conveyer;
		}
		return conveyers;
	}
	
	/**
	 * 親要素から親工程を生成して返します。
	 * @param parent 親要素
	 * @return 親工程
	 */
	Conveyer<String> getConveyer(TpacParent parent){
		Conveyer<String> conveyer = null;
		switch (parent.tag){
			case 'slice':
				conveyer = new SliceConveyer<String>(parent);
				break;
			case 'between':
				conveyer = new BetweenConveyer<String>(parent);
				break;
			default:
				// 上記以外のタグは無視します
				break;
		}
		return conveyer;
	}
	
	/**
	 * 文字列を一時的に保持するためのクラスです。<br>
	 * 一世代過去の文字列も保持します。
	 */
	protected class TextBuffer {
		/** 対象文字列 */
		List<String> lines = [];
		/** 対象外文字列 */
		List<String> others = [];
		/** 前回の対象文字列 */
		List<String> preLines = [];
		/** 前回の対象外文字列 */
		List<String> preOthers = [];
		
		/**
		 * 一世代前に文字列を移動します。
		 */
		TextBuffer trans(){
			TextBuffer buffer = new TextBuffer();
			buffer.preLines.addAll(lines);
			buffer.preOthers.addAll(others);
			return buffer;
		}
	}
	
	/**
	 * 内部に子工程を持つ工程の抽象クラスです。
	 */
	abstract class InternalConveyer<String> extends Conveyer {
		/** ConveyerSystem */
		ConveyerSystem<List<String>> internalSystem = null;
		/** 子工程を実行しているスレッド */
		JavaThread pushThread = null;
		/** 子工程から処理結果をとりだすスレッド */
		JavaThread takeThead = null;
		/** 文字列退避用バッファ */
		TextBuffer buffer = null;
		/** divhandle属性 */
		String divhandle = null;
		/** judge属性 */
		String judgeCombi = null;
		/** 位置 */
		int index;
		
		/**
		 * 子工程を開始します。
		 */
		protected void initInternalConveyer(TpacParent parent){
			internalSystem = new ConveyerSystem<List<String>>(new ChildConveyer(clmap).getConveyers(parent.lowers.values() as List));
			pushThread = internalSystem.conveyAsync();
			takeThead = Thread.start {
				try {
					List<String> lines;
					while ((lines = internalSystem.queueOut.take().elem) != null){
						lines.each { String line -> pass(new Product(line)) }
					}
				} catch (InterruptedException exc){
					ParentConveyer.LOG.info('割り込みが発生したため、要素の取得を終了します。');
				} catch (exc){
					ParentConveyer.LOG.error('要素の取得時に例外が発生しました。', exc);
				}
			}
			index = 0;
		}
		
		/**
		 * 子工程に要素を渡します。
		 * @param divs 区切り行のリスト
		 * @param buffer 範囲内の行のリスト
		 * @param isLast 最後の要素か
		 */
		protected void passInternalConveyer(List<String> divs, List<String> buffer, boolean isLast){
			if (divs.size() > 0) internalSystem.passThrough(divs);
			if (buffer.size() > 0){
				ParentConveyer.LOG.debug('子工程に格納 {}: {}', name, buffer);
				clmap.config."${name}".index = index;
				clmap.config."${name}".isLast = isLast;
				if (judgeCombi != null && !clmap.cl(judgeCombi).call(buffer)){
					internalSystem.passThrough(buffer);
				} else {
					internalSystem.pass(new IndexedProduct<List<String>>(buffer, true, index, isLast) as Product);
				}
				index ++;
			}
		}
		
		/**
		 * 終期化処理として、子工程の処理を完了します。
		 */
		@Override
		void terminate(){
			ParentConveyer.LOG.debug('子工程を完了 {}', name);
			internalSystem.complete();
			pushThread.join();
			takeThead.join();
			if (internalSystem.exc != null) throw internalSystem.exc;
		}
	}
	
	/**
	 * sliceタグの工程です。
	 */
	class SliceConveyer<String> extends InternalConveyer {
		/** div属性 */
		String div = null;
		
		/**
		 * コンストラクタ。
		 * @param parent 親要素
		 */
		SliceConveyer(TpacParent parent){
			this.name = parent.key;
			this.div = parent.attrs['div'] ?: /^$/;
			this.divhandle = (parent.attrs['divhandle'] != null)? parent.attrs['divhandle'] : 'exclude';
			this.judgeCombi = parent.attrs['judge'];
			if (this.judgeCombi != null) ArgmentChecker.checkNotNull('judge属性に該当するクロージャ定義', clmap.cl(this.judgeCombi));
			buffer = new TextBuffer();
			initInternalConveyer(parent);
		}
		
		/** {@inheritDoc} */
		void convey(Product<String> product){
			try {
				if (product.elem.matches(div)){
					if (buffer.lines.size() > 0){
						// 子工程に文字列を渡します
						passInternalConveyer(buffer.preOthers, buffer.preLines, false);
						// 文字列を退避しておきます
						buffer = buffer.trans();
					}
					switch (divhandle){	// 区切り文字列を格納／除外／削除します
						case 'include': buffer.lines << product.elem; break;
						case 'exclude': buffer.others << product.elem; break;
						case 'delete': break;
					}
				} else {
					buffer.lines << product.elem;
				}
			} catch (exc){
				LOG.warn('工程で問題が発生しました。name={}', name);
				throw exc;
			}
		}
		
		/** {@inheritDoc} */
		void conveyLast(){
			try {
				// まだ子工程に渡していない文字列が残っていれば渡します
				passInternalConveyer(buffer.preOthers, buffer.preLines, (buffer.lines.size() == 0)? true : false);
				passInternalConveyer(buffer.others, buffer.lines, true);
			} catch (exc){
				LOG.warn('最後の工程で問題が発生しました。name={}', name);
				throw exc;
			}
		}
	}
	
	/**
	 * betweenタグの工程です。
	 */
	class BetweenConveyer<String> extends InternalConveyer {
		/** bgn属性 */
		String bgn = null;
		/** end属性 */
		String end = null;
		/** 区切り行内を走査中か */
		boolean inDivs = false;
		/** 開始行 */
		String bgnLine = '';
		
		/**
		 * コンストラクタ。
		 * @param parent 親要素
		 */
		BetweenConveyer(TpacParent parent){
			this.name = parent.key;
			this.bgn = parent.attrs['bgn'];
			ArgmentChecker.checkNotNull('bgn属性', this.bgn);
			this.end = parent.attrs['end'] ?: '';
			this.divhandle = (parent.attrs['divhandle'] != null)? parent.attrs['divhandle'] : 'exclude';
			this.judgeCombi = parent.attrs['judge'];
			if (this.judgeCombi != null) ArgmentChecker.checkNotNull('judge属性に該当するクロージャ定義', clmap.cl(this.judgeCombi));
			buffer = new TextBuffer();
			initInternalConveyer(parent);
		}
		
		/** {@inheritDoc} */
		void convey(Product<String> product){
			try {
				if (inDivs){
					if ((end.empty && product.elem == bgnLine) || (!end.empty && product.elem.matches(end))){
						inDivs = false;
						if (divhandle == 'include') buffer.lines << product.elem;
						// 子工程に文字列を渡します
						passInternalConveyer(buffer.preOthers, buffer.preLines, false);
						// 文字列を退避しておきます
						buffer = buffer.trans();
						if (divhandle == 'exclude') buffer.others << product.elem;
					} else {
						buffer.lines << product.elem;
					}
				} else {
					if (product.elem.matches(bgn)){
						bgnLine = product.elem;
						inDivs = true;
						buffer.lines.clear();
						switch (divhandle){	// 区切り文字列を格納／除外／削除します
							case 'include': buffer.lines << product.elem; break;
							case 'exclude': buffer.others << product.elem; break;
							case 'delete': break;
						}
					} else {
						buffer.others << product.elem;
					}
				}
			} catch (exc){
				LOG.warn('工程で問題が発生しました。name={}', name);
				throw exc;
			}
		}
		
		/** {@inheritDoc} */
		void conveyLast(){
			try {
				// まだ子工程に渡していない文字列が残っていれば渡します
				passInternalConveyer(buffer.preOthers, buffer.preLines, (buffer.lines.size() == 0)? true : false);
				passInternalConveyer(buffer.others, buffer.lines, true);
			} catch (exc){
				LOG.warn('最後の工程で問題が発生しました。name={}', name);
				throw exc;
			}
		}
	}
}
