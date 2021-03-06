package cn.com.kongfupanda.solr.analyzers;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.nlpcn.commons.lang.pinyin.Pinyin;

/*
 * Author: Jason Song
 *  Email: Jasonsong74@126.com
 */
public final class NlpPinyinTokenFilter extends TokenFilter {
	private int _mintermlen = 2;
	private boolean _mixpinyin = false;
	private boolean _firstchar = false;
	private boolean _isoutchinese = true;
	private boolean _combination = false;
	
	private char[] curTermBuffer;
	private int curTermLength;
	private final CharTermAttribute termAtt = (CharTermAttribute) addAttribute(CharTermAttribute.class);
	private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
	private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);
	private boolean hasCurOut = false;

	private Collection<String> terms = null;
	private Iterator<String> termIterator = null;

	protected NlpPinyinTokenFilter(TokenStream input) {
		this(input, 2);
	}

	protected NlpPinyinTokenFilter(TokenStream input, int mintermlen) {
		this(input, mintermlen, false);
	}

	protected NlpPinyinTokenFilter(TokenStream input, int mintermlen,
			boolean mixpinyin) {
		this(input, mintermlen, mixpinyin, false);
	}

	protected NlpPinyinTokenFilter(TokenStream input, int mintermlen,
			boolean mixpinyin, boolean firstchar) {
		this(input, mintermlen, mixpinyin, firstchar, true);
	}

	protected NlpPinyinTokenFilter(TokenStream input, int mintermlen,
			boolean mixpinyin, boolean firstchar, boolean outchinese) {
		this(input, mintermlen, mixpinyin, firstchar, outchinese, false);
	}
	
	protected NlpPinyinTokenFilter(TokenStream input, int mintermlen,
			boolean mixpinyin, boolean firstchar, boolean outchinese, boolean combination) {
		super(input);
		_mintermlen = mintermlen;
		_mixpinyin = mixpinyin;
		_firstchar = firstchar;
		_isoutchinese = outchinese;
		_combination = combination;
		addAttribute(OffsetAttribute.class); // 偏移量属性
	}

	private static int ChineseCharCount(String s) {
		int count = 0;
		int stringlen = s.length();
		if ((null == s) || ("".equals(s.trim())))
			return count;
		for (int i = 0; i < stringlen; i++) {
			int codepoint = s.codePointAt(i);
			if (Character.UnicodeScript.of(codepoint) == Character.UnicodeScript.HAN)
				count++;
		}
		return count;
	}

	public void reset() throws IOException {
		super.reset();
	}

	/**
	 * http://stackoverflow.com/questions/599161/best-way-to-convert-an-
	 * arraylist-to-a-string
	 */
	private static Collection<String> BuildFullPinyinString(List<String> pinyinlist,
			String chinese) {
		String pinyinbuilder = "";
		Set<String> pinyins = null;
		int i = 0;
		for (String array : pinyinlist) {
			if (array != null) {
				pinyinbuilder += array;
			} else {
				pinyinbuilder += chinese.charAt(i);
			}
			i++;
		}
		if (!"".equals(pinyinbuilder)) {
			pinyins = new HashSet<String>();
			pinyins.add(pinyinbuilder);
		}
		return pinyins;
	}

	/*
	 * For short pinyin, whatever the value _mixpinyin is, in this function
	 * _mixpinyin will be treated as default value(false)
	 */
	private static Collection<String> BuildShortPinyinString(List<String> pinyinlist,
			String chinese) {
		Set<String> pinyins = null;
		String allfirstchar = "";
		for (String array : pinyinlist) {
			if (pinyins == null || pinyins.isEmpty()) {
				pinyins = new HashSet<String>();
			}

			if (array != null) {
				pinyins.add(array);
				allfirstchar += array;
			}
		}
		if (pinyins != null)
			pinyins.add(allfirstchar);
		return pinyins;
	}

	private Collection<String> GetPyString(String chinese) {
		List<String> pinyinlist;
		if (this._firstchar) {
			pinyinlist = Pinyin.firstChar(chinese);
			if (this._combination) {
				return PinyinCombineSkipNull(pinyinlist);
			}
			else
				return BuildShortPinyinString(pinyinlist, chinese);
		} else {
			pinyinlist = Pinyin.pinyin(chinese);
			if (!this._mixpinyin)
				return PinyinCombineSkipNull(pinyinlist);
			else
				return BuildFullPinyinString(pinyinlist, chinese);
		}
	}

	public Collection<String> PinyinCombineSkipNull(List<String> pinyinlist) {
		String combinationPinyin = Pinyin.list2StringSkipNull(pinyinlist,"");
		Set<String> pinyins = new HashSet<String>();
		pinyins.add(combinationPinyin);
		return pinyins;
	}

	@Override
	public boolean incrementToken() throws IOException {
		while (true) {
			if (this.curTermBuffer == null) {
				if (!this.input.incrementToken()) {
					return false;
				}
				this.curTermBuffer = ((char[]) this.termAtt.buffer().clone());
				this.curTermLength = this.termAtt.length();
			}
			if ((this._isoutchinese) && (!this.hasCurOut)) {
				this.hasCurOut = true;
				this.termAtt.copyBuffer(this.curTermBuffer, 0,
						this.curTermLength);
				this.posIncrAtt.setPositionIncrement(this.posIncrAtt
						.getPositionIncrement());
				return true;
			}
			String chinese = this.termAtt.toString();
			if (ChineseCharCount(chinese) >= this._mintermlen) {
				this.terms = GetPyString(chinese);
				if (this.terms != null)
					this.termIterator = this.terms.iterator();
			}			
			if (this.termIterator != null) {
				while (this.termIterator.hasNext()) {
					String pinyin = this.termIterator.next();
					this.termAtt.copyBuffer(pinyin.toCharArray(), 0,
							pinyin.length());
					this.posIncrAtt.setPositionIncrement(0);
					this.typeAtt.setType(this._firstchar ? "short_pinyin"
							: "pinyin");
					return true;
				}
			}			
			this.curTermBuffer = null;
			this.termIterator = null;
			this.hasCurOut = false; // 下次取词元后输出原词元（如果开关也准许）
		}
	}
}
