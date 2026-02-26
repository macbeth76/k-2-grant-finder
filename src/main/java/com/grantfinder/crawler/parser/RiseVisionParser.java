package com.grantfinder.crawler.parser;

import com.grantfinder.crawler.CrawlSource;
import jakarta.inject.Singleton;

@Singleton
public class RiseVisionParser extends WordPressGrantPageParser {

    @Override
    public CrawlSource getSource() { return CrawlSource.RISEVISION; }

    @Override
    protected String getUrl() { return "https://www.risevision.com/blog/40-grants-to-fund-technology-for-your-school"; }

    @Override
    protected String getSiteName() { return "Rise Vision"; }

    @Override
    protected String getDefaultCategory() { return "RESEARCH"; }
}
