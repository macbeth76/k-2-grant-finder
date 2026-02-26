package com.grantfinder.crawler.parser;

import com.grantfinder.crawler.CrawlSource;
import jakarta.inject.Singleton;

@Singleton
public class NextWaveStemParser extends WordPressGrantPageParser {

    @Override
    public CrawlSource getSource() { return CrawlSource.NEXTWAVESTEM; }

    @Override
    protected String getUrl() { return "https://nextwavestem.com/stem-grants-for-teachers"; }

    @Override
    protected String getSiteName() { return "NextWaveSTEM"; }

    @Override
    protected String getDefaultCategory() { return "RESEARCH"; }
}
