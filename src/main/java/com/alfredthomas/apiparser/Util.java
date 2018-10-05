package com.alfredthomas.apiparser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.*;

class Util {


    static Document readRemoteHTMLFile(String path)
    {
        try{
            return Jsoup.connect(path).get();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }
    static Document getAllClassDocument(String currentPath) throws FileNotFoundException
    {
        Document currentPage;
        String url = null;

        try {
        //for normal doc pages
        if(currentPath.endsWith(".html")) {
            currentPage = Util.readRemoteHTMLFile(currentPath);
            if (currentPage != null) {
                Element allClassNavButton = currentPage.getElementById("allclasses_navbar_top").getElementsByTag("a").first();

                url = allClassNavButton.attr("abs:href");
            }
        }
        //frame layout urls don't change
        else {
            Document frameDocument = Util.readRemoteHTMLFile(currentPath);
            if (frameDocument != null) {
                url = frameDocument.getElementsByAttributeValue("name","packageFrame").first().attr("abs:src");
            }
        }

        return Util.readRemoteHTMLFile(url);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new FileNotFoundException("Cannot find allclasses file");
        }
    }
    static String sanitizeURL(String current)
    {
//        StringBuilder stringBuilder = new StringBuilder(current);
//        while( stringBuilder.substring(0,2).equals("../"))
//        {
//            stringBuilder.delete(0,2);
//        }
        if(current.isEmpty())
            return current;
        int startIndex = 0;
        while(startIndex< current.length()-3 && current.substring(startIndex,startIndex+3).equals("../"))
        {
            startIndex+=3;
        }

        String sanitized = current.substring(startIndex);
        if(sanitized.endsWith(".html"))
            sanitized = sanitized.substring(0,sanitized.lastIndexOf('.'));

        return sanitized;
    }
}
