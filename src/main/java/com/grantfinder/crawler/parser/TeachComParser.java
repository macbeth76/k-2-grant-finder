package com.grantfinder.crawler.parser;

import com.grantfinder.crawler.CrawlSource;
import jakarta.inject.Singleton;

@Singleton
public class TeachComParser extends WordPressGrantPageParser {

    @Override
    public CrawlSource getSource() { return CrawlSource.TEACH_COM; }

    @Override
    protected String getUrl() { return "https://teach.com/what/teachers-change-lives/grants-for-teachers/"; }

    @Override
    protected String getSiteName() { return "Teach.com"; }

    @Override
    protected String getDefaultCategory() { return "CHILDREN"; }
}
