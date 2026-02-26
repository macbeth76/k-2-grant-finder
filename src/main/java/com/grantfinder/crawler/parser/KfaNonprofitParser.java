package com.grantfinder.crawler.parser;

import com.grantfinder.crawler.CrawlSource;
import jakarta.inject.Singleton;

@Singleton
public class KfaNonprofitParser extends WordPressGrantPageParser {

    @Override
    public CrawlSource getSource() { return CrawlSource.KFA_NONPROFIT; }

    @Override
    protected String getUrl() { return "https://kfanonprofit.com/blogs/blog/teacher-grants-2025"; }

    @Override
    protected String getSiteName() { return "KFA Nonprofit"; }

    @Override
    protected String getDefaultCategory() { return "CHILDREN"; }
}
