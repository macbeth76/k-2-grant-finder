package com.grantfinder.crawler.parser;

import com.grantfinder.crawler.CrawlSource;
import jakarta.inject.Singleton;

@Singleton
public class ChildrensMusicWorkshopParser extends WordPressGrantPageParser {

    @Override
    public CrawlSource getSource() { return CrawlSource.CHILDRENSMUSICWORKSHOP; }

    @Override
    protected String getUrl() { return "https://www.childrensmusicworkshop.com/resources/grants/"; }

    @Override
    protected String getSiteName() { return "Children's Music Workshop"; }

    @Override
    protected String getDefaultCategory() { return "POETRY"; } // Arts category
}
