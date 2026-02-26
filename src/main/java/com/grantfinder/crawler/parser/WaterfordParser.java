package com.grantfinder.crawler.parser;

import com.grantfinder.crawler.CrawlSource;
import jakarta.inject.Singleton;

@Singleton
public class WaterfordParser extends WordPressGrantPageParser {

    @Override
    public CrawlSource getSource() { return CrawlSource.WATERFORD; }

    @Override
    protected String getUrl() { return "https://www.waterford.org/blog/educational-grants/"; }

    @Override
    protected String getSiteName() { return "Waterford.org"; }

    @Override
    protected String getDefaultCategory() { return "CHILDREN"; }
}
