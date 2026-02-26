package com.grantfinder.crawler.parser;

import com.grantfinder.crawler.CrawlSource;
import jakarta.inject.Singleton;

@Singleton
public class ProgressLearningParser extends WordPressGrantPageParser {

    @Override
    public CrawlSource getSource() { return CrawlSource.PROGRESSLEARNING; }

    @Override
    protected String getUrl() { return "https://progresslearning.com/news-blog/find-tech-grants-for-schools-districts/"; }

    @Override
    protected String getSiteName() { return "Progress Learning"; }

    @Override
    protected String getDefaultCategory() { return "RESEARCH"; }
}
