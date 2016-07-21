package cn.com.kongfupanda.solr.analyzers;

import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.solr.analysis.BaseTokenFilterFactory;

/*
 * Author: Jason Song
 *  Email: Jasonsong74@126.com
 */
public class NlpPinyinTokenFilterFactory extends BaseTokenFilterFactory {
	private int _mintermlen = 2;  
	private boolean _mixpinyin = false;   // default is not mixed(false)
	private boolean _firstchar = false;   // A switch flag for shot/full PINYI	
	private boolean _isoutchinese = true; // A switch flag for Chinese term output
    private boolean _combination = true; //首字母组合    First letter combinations,only works under _firstchar =true    
	
	@Override
	public void init(Map<String, String> args) {
		super.init(args);
		assureMatchVersion();

		_mintermlen = getInt(args, "minTermLen", 2);
		if (this._mintermlen < 1) {
			this._mintermlen = 1;
		}
		_mixpinyin = getBoolean(args, "mixPinyin", false);
		_firstchar = getBoolean(args, "firstChar", false);
		_isoutchinese = getBoolean(args, "outChinese", true);
		if (_firstchar)
			_combination = getBoolean(args, "firstCombination", true);
	}

	public TokenStream create(TokenStream input) {
		return new NlpPinyinTokenFilter(input, this._mintermlen,
				this._mixpinyin, this._firstchar, this._isoutchinese,this._combination);
	}

	private final int getInt(Map<String, String> args, String name,
			int defaultVal) {
		String s = args.remove(name);
		return s == null ? defaultVal : Integer.parseInt(s);
	}

	private final boolean getBoolean(Map<String, String> args, String name,
			boolean defaultVal) {
		String s = args.remove(name);
		return s == null ? defaultVal : Boolean.parseBoolean(s);
	}
}
