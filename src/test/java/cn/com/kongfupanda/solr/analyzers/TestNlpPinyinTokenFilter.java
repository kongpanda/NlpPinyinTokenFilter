package cn.com.kongfupanda.solr.analyzers;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import cn.com.kongfupanda.solr.analyzers.NlpPinyinTokenFilter;

/*
 * Need add more test cases  
 */
@SuppressWarnings("unused")
@RunWith(com.carrotsearch.randomizedtesting.RandomizedRunner.class)
public class TestNlpPinyinTokenFilter {
	private NlpPinyinTokenFilter filter;

	@Before
	public void before() {
		// "和平 重量 and 中国  12山东34   上海123" failure
		// "和平 重量 and 中国  山东 上海" success
		// MockTokenizer tokenizer = new MockTokenizer(new
		// StringReader("和平 重量 and 中国  12山东34   上海123"));
		// this.filter = new PinyinTokenFilter(tokenizer,true, true,2,false);
		MockTokenizer tokenizer = new MockTokenizer(new StringReader(
				"和平 重量 and 中国  12山东34   上海123  上——海123"), 0, false);
		this.filter = new NlpPinyinTokenFilter(tokenizer, 2, false, false,true);
	}

	@Test
	public void test() throws IOException {
		this.filter.reset();
		int position = 0;
		while (this.filter.incrementToken()) {
			CharTermAttribute termAtt = this.filter
					.getAttribute(CharTermAttribute.class);
			String token = termAtt.toString();
			int increment = this.filter.getAttribute(
					PositionIncrementAttribute.class).getPositionIncrement();
			position += increment;
			OffsetAttribute offset = this.filter
					.getAttribute(OffsetAttribute.class);
			TypeAttribute type = this.filter.getAttribute(TypeAttribute.class);
			System.out.println(position + "[" + offset.startOffset() + ","
					+ offset.endOffset() + "} (" + type.type() + ") " + token);
		}
	}
}
