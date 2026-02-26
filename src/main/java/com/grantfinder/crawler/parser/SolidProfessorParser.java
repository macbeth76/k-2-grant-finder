package com.grantfinder.crawler.parser;

import com.grantfinder.crawler.CrawlSource;
import jakarta.inject.Singleton;

@Singleton
public class SolidProfessorParser extends WordPressGrantPageParser {

    @Override
    public CrawlSource getSource() { return CrawlSource.SOLIDPROFESSOR; }

    @Override
    protected String getUrl() { return "https://solidprofessor.com/blog/14-professional-development-grants-for-teachers/"; }

    @Override
    protected String getSiteName() { return "SolidProfessor"; }

    @Override
    protected String getDefaultCategory() { return "RESEARCH"; }
}
