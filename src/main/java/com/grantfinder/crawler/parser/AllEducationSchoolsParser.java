package com.grantfinder.crawler.parser;

import com.grantfinder.crawler.CrawlSource;
import jakarta.inject.Singleton;

@Singleton
public class AllEducationSchoolsParser extends WordPressGrantPageParser {

    @Override
    public CrawlSource getSource() { return CrawlSource.ALLEDUCATIONSCHOOLS; }

    @Override
    protected String getUrl() { return "https://www.alleducationschools.com/blog/grants-for-teachers/"; }

    @Override
    protected String getSiteName() { return "All Education Schools"; }

    @Override
    protected String getDefaultCategory() { return "CHILDREN"; }
}
