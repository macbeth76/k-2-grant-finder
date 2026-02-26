package com.grantfinder.crawler.parser;

import com.grantfinder.crawler.CrawlSource;
import jakarta.inject.Singleton;

@Singleton
public class OzobotParser extends WordPressGrantPageParser {

    @Override
    public CrawlSource getSource() { return CrawlSource.OZOBOT; }

    @Override
    protected String getUrl() { return "https://ozobot.com/19-grants-for-teachers-to-fund-steam-classrooms-and-projects/"; }

    @Override
    protected String getSiteName() { return "Ozobot"; }

    @Override
    protected String getDefaultCategory() { return "RESEARCH"; }
}
