package com.grantfinder.crawler.parser;

import com.grantfinder.crawler.CrawlSource;
import jakarta.inject.Singleton;

@Singleton
public class Connect4EducationParser extends WordPressGrantPageParser {

    @Override
    public CrawlSource getSource() { return CrawlSource.CONNECT4EDUCATION; }

    @Override
    protected String getUrl() { return "https://connect4education.com/10-music-education-grants-you-can-apply-for-in-2024/"; }

    @Override
    protected String getSiteName() { return "Connect4Education"; }

    @Override
    protected String getDefaultCategory() { return "POETRY"; } // Arts category
}
