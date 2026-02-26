package com.grantfinder.crawler.parser;

import com.grantfinder.crawler.CrawlSource;
import jakarta.inject.Singleton;

@Singleton
public class PlanbookParser extends WordPressGrantPageParser {

    @Override
    public CrawlSource getSource() { return CrawlSource.PLANBOOK; }

    @Override
    protected String getUrl() { return "https://blog.planbook.com/technology-grants/"; }

    @Override
    protected String getSiteName() { return "Planbook"; }

    @Override
    protected String getDefaultCategory() { return "RESEARCH"; }
}
