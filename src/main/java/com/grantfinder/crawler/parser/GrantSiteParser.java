package com.grantfinder.crawler.parser;

import com.grantfinder.crawler.CrawlSource;
import com.grantfinder.model.Grant;

import java.io.IOException;
import java.util.List;

public interface GrantSiteParser {

    CrawlSource getSource();

    List<Grant> parse() throws IOException;
}
