package com.grantfinder.crawler.parser;

import com.grantfinder.crawler.CrawlSource;
import jakarta.inject.Singleton;

@Singleton
public class NeaFoundationParser extends WordPressGrantPageParser {

    @Override
    public CrawlSource getSource() { return CrawlSource.NEA_FOUNDATION; }

    @Override
    protected String getUrl() { return "https://www.neafoundation.org/educator-grants-and-fellowships/other-grant-fellowship-opportunities/"; }

    @Override
    protected String getSiteName() { return "NEA Foundation"; }

    @Override
    protected String getDefaultCategory() { return "CHILDREN"; }
}
